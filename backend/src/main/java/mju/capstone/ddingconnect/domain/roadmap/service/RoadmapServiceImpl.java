package mju.capstone.ddingconnect.domain.roadmap.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.member.domain.repository.MemberRepository;
import mju.capstone.ddingconnect.domain.roadmap.domain.Roadmap;
import mju.capstone.ddingconnect.domain.roadmap.domain.RoadmapAlarm;
import mju.capstone.ddingconnect.domain.roadmap.domain.repository.RoadmapAlarmRepository;
import mju.capstone.ddingconnect.domain.roadmap.domain.repository.RoadmapRepository;
import mju.capstone.ddingconnect.domain.roadmap.dto.request.CreateRoadmapRequest;
import mju.capstone.ddingconnect.domain.roadmap.dto.response.RoadmapDownloadResponse;
import mju.capstone.ddingconnect.domain.roadmap.dto.response.RoadmapListResponse;
import mju.capstone.ddingconnect.domain.roadmap.dto.response.RoadmapResponse;
import mju.capstone.ddingconnect.global.aws.S3Service;
import mju.capstone.ddingconnect.global.response.code.status.ErrorStatus;
import mju.capstone.ddingconnect.global.response.exception.handler.MemberHandler;
import mju.capstone.ddingconnect.global.response.exception.handler.RoadmapHandler;
import mju.capstone.ddingconnect.global.sse.AlarmNotificationEvent;
import mju.capstone.ddingconnect.global.alarm.AlarmType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoadmapServiceImpl implements RoadmapService {

    // 테스트도 동일 상수를 참조하므로 package-private 노출
    static final String ROADMAP_ALARM_CONTENT = "로드맵 생성이 완료되었습니다.";

    /** S3 키 prefix — {prefix}{roadmapId}.pdf 형태로 저장. 결정론적이라 별도 컬럼 불필요. */
    static final String S3_KEY_PREFIX = "roadmaps/";
    static final String PDF_CONTENT_TYPE = "application/pdf";

    /** 파일명 sanitize — Windows/macOS 금지 문자 + 제어문자 제거. */
    private static final Pattern UNSAFE_FILENAME_CHARS = Pattern.compile("[\\\\/:*?\"<>|\\x00-\\x1F]");

    private final RoadmapRepository roadmapRepository;
    private final RoadmapAlarmRepository roadmapAlarmRepository;
    private final MemberRepository memberRepository;
    private final RoadmapAiClient roadmapAiClient;
    private final ApplicationEventPublisher eventPublisher;
    private final RoadmapPdfRenderer roadmapPdfRenderer;
    private final S3Service s3Service;

    /** application-s3.yml 의 aws.s3.download-presign-ttl (ISO-8601 duration, 기본 5분). */
    @Value("${aws.s3.download-presign-ttl:PT5M}")
    private Duration downloadPresignTtl;

    @Override
    @Transactional
    public RoadmapResponse create(Long memberId, CreateRoadmapRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));

        // 데이터 파트가 AI 로드맵 생성을 담당하고, 백엔드는 그 결과 JSON 만 저장한다.
        String content = roadmapAiClient.generate(request, memberId);
        validateContent(content);

        Roadmap roadmap = Roadmap.builder()
                .member(member)
                .content(content)
                .build();
        Roadmap saved = roadmapRepository.save(roadmap);

        // 생성 직후 1회만 PDF 렌더 + S3 업로드. 다운로드 요청 시점엔 S3 객체를 재사용한다.
        // 외부 IO 가 @Transactional 안에 있는 점은 RoadmapAiClient.generate 와 동일 패턴 (실패 시 본체 저장도 롤백).
        byte[] pdf = roadmapPdfRenderer.render(content);
        s3Service.uploadBytes(pdf, buildKey(saved.getId()), PDF_CONTENT_TYPE);

        // [알람 발행] 본인(생성자)에게 1건
        roadmapAlarmRepository.save(RoadmapAlarm.builder()
                .roadmap(saved)
                .content(ROADMAP_ALARM_CONTENT)
                .isRead(false)
                .build());
        eventPublisher.publishEvent(new AlarmNotificationEvent(
                member, AlarmType.ROADMAP, ROADMAP_ALARM_CONTENT));

        return RoadmapResponse.from(saved);
    }

    /** 데이터 파트 AI 응답 본문 검증 — null/blank 면 INVALID_CONTENT. */
    private void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new RoadmapHandler(ErrorStatus.ROADMAP_INVALID_CONTENT);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoadmapListResponse> getList(Member member) {
        return roadmapRepository.findByMemberIdOrderByCreatedAtDesc(member.getId())
                .stream().map(RoadmapListResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RoadmapResponse getOne(Member member, Long roadmapId) {
        Roadmap roadmap = roadmapRepository.findById(roadmapId)
                .orElseThrow(() -> new RoadmapHandler(ErrorStatus.ROADMAP_NOT_FOUND));
        if (!roadmap.getMember().getId().equals(member.getId())) {
            throw new RoadmapHandler(ErrorStatus.ROADMAP_UNAUTHORIZED);
        }
        return RoadmapResponse.from(roadmap);
    }

    @Override
    @Transactional
    public void delete(Member member, Long roadmapId) {
        Roadmap roadmap = roadmapRepository.findById(roadmapId)
                .orElseThrow(() -> new RoadmapHandler(ErrorStatus.ROADMAP_NOT_FOUND));

        if (!roadmap.getMember().getId().equals(member.getId())) {
            throw new RoadmapHandler(ErrorStatus.ROADMAP_UNAUTHORIZED);
        }

        // Roadmap 을 NOT NULL FK 로 참조하는 RoadmapAlarm 먼저 삭제 (FK 제약 / TransientObjectException 방지)
        roadmapAlarmRepository.deleteByRoadmapId(roadmapId);
        roadmapRepository.delete(roadmap);

        // S3 객체 삭제는 best-effort — 실패해도 트랜잭션엔 영향 없음. dangling 객체는 추후 cleanup job 으로 정리 대상.
        try {
            s3Service.deleteImage(buildKey(roadmapId));
        } catch (Exception e) {
            log.warn("로드맵 PDF S3 삭제 실패 — roadmapId={}, key={}", roadmapId, buildKey(roadmapId), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long countMyRoadmaps(Member member) {
        return roadmapRepository.countByMemberId(member.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public RoadmapDownloadResponse getDownloadUrl(Member member, Long roadmapId) {
        Roadmap roadmap = roadmapRepository.findById(roadmapId)
                .orElseThrow(() -> new RoadmapHandler(ErrorStatus.ROADMAP_NOT_FOUND));

        if (!roadmap.getMember().getId().equals(member.getId())) {
            throw new RoadmapHandler(ErrorStatus.ROADMAP_UNAUTHORIZED);
        }

        String fileName = buildFileName(roadmap.getContent(), roadmapId);
        String key = buildKey(roadmapId);
        String url = s3Service.generatePresignedUrl(key, downloadPresignTtl, fileName);
        LocalDateTime expiresAt = LocalDateTime.now().plus(downloadPresignTtl);

        return new RoadmapDownloadResponse(url, fileName, expiresAt);
    }

    static String buildKey(Long roadmapId) {
        return S3_KEY_PREFIX + roadmapId + ".pdf";
    }

    /**
     * 로드맵 content(JSON) 에서 {@code roadmap_title} 을 뽑아 파일명으로 만든다.
     * 파싱 실패하거나 제목이 비어 있으면 "roadmap-{id}.pdf" 로 폴백.
     */
    static String buildFileName(String content, Long roadmapId) {
        String title = extractTitle(content);
        if (title == null || title.isBlank()) {
            return "roadmap-" + roadmapId + ".pdf";
        }
        String safe = UNSAFE_FILENAME_CHARS.matcher(title).replaceAll("").trim();
        if (safe.isEmpty()) {
            return "roadmap-" + roadmapId + ".pdf";
        }
        return safe + ".pdf";
    }

    private static String extractTitle(String content) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readTree(content)
                    .path("roadmap_title")
                    .asText(null);
        } catch (Exception e) {
            return null;
        }
    }
}
