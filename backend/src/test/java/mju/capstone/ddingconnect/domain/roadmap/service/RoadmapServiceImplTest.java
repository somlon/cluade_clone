package mju.capstone.ddingconnect.domain.roadmap.service;

import mju.capstone.ddingconnect.domain.interested_job.domain.TargetJobCategory;
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
import mju.capstone.ddingconnect.domain.techstack.domain.TechStackName;
import mju.capstone.ddingconnect.global.alarm.AlarmType;
import mju.capstone.ddingconnect.global.aws.S3Service;
import mju.capstone.ddingconnect.global.response.exception.handler.MemberHandler;
import mju.capstone.ddingconnect.global.response.exception.handler.RoadmapHandler;
import mju.capstone.ddingconnect.global.sse.AlarmNotificationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoadmapServiceImpl 단위 테스트")
class RoadmapServiceImplTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long ROADMAP_ID = 10L;
    private static final String GENERATED_CONTENT = "{\"roadmap_title\":\"백엔드 개발자 로드맵\",\"steps\":[]}";
    private static final byte[] FAKE_PDF = "%PDF-1.4 fake".getBytes();
    private static final Duration PRESIGN_TTL = Duration.ofMinutes(5);

    @Mock RoadmapRepository roadmapRepository;
    @Mock RoadmapAlarmRepository roadmapAlarmRepository;
    @Mock MemberRepository memberRepository;
    @Mock RoadmapAiClient roadmapAiClient;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock RoadmapPdfRenderer roadmapPdfRenderer;
    @Mock S3Service s3Service;
    @InjectMocks RoadmapServiceImpl roadmapService;

    private Member author;
    private Member other;
    private Roadmap roadmap;

    @BeforeEach
    void setUp() {
        author = Member.builder().id(MEMBER_ID).email("a@mju.ac.kr").nickname("작성자").build();
        other = Member.builder().id(2L).email("o@mju.ac.kr").nickname("타인").build();
        roadmap = Roadmap.builder().id(ROADMAP_ID).member(author).content(GENERATED_CONTENT).build();
        // @Value 주입 필드는 Mockito @InjectMocks 가 채우지 못하므로 직접 세팅
        ReflectionTestUtils.setField(roadmapService, "downloadPresignTtl", PRESIGN_TTL);
    }

    private CreateRoadmapRequest sampleRequest() {
        return new CreateRoadmapRequest(3, 4.0, "응용소프트웨어학과",
                TargetJobCategory.BACKEND, List.of(TechStackName.JAVA, TechStackName.SPRING), "카카오");
    }

    @Test
    @DisplayName("create - 데이터 파트 AI 를 호출해 응답을 저장하고 본인에게 RoadmapAlarm 1건 발행한다")
    void createCallsAiClientAndPublishesAlarm() {
        CreateRoadmapRequest req = sampleRequest();
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(author));
        when(roadmapAiClient.generate(req, MEMBER_ID)).thenReturn(GENERATED_CONTENT);
        when(roadmapRepository.save(any(Roadmap.class))).thenReturn(roadmap);
        when(roadmapPdfRenderer.render(GENERATED_CONTENT)).thenReturn(FAKE_PDF);

        RoadmapResponse response = roadmapService.create(MEMBER_ID, req);

        assertThat(response.id()).isEqualTo(ROADMAP_ID);
        assertThat(response.content()).isEqualTo(GENERATED_CONTENT);
        verify(roadmapAiClient).generate(req, MEMBER_ID);

        // 저장된 Roadmap.content 는 요청값이 아닌 데이터 파트 AI 응답값이어야 한다
        ArgumentCaptor<Roadmap> roadmapCaptor = ArgumentCaptor.forClass(Roadmap.class);
        verify(roadmapRepository).save(roadmapCaptor.capture());
        assertThat(roadmapCaptor.getValue().getContent()).isEqualTo(GENERATED_CONTENT);

        // 생성 직후 PDF 렌더 + S3 업로드 — 키 규약: roadmaps/{id}.pdf
        verify(roadmapPdfRenderer).render(GENERATED_CONTENT);
        verify(s3Service).uploadBytes(FAKE_PDF, "roadmaps/" + ROADMAP_ID + ".pdf", "application/pdf");

        verify(roadmapAlarmRepository).save(any(RoadmapAlarm.class));

        // 커밋 후 SSE 푸시용 이벤트가 로드맵 생성자 본인 대상으로 발행된다
        ArgumentCaptor<AlarmNotificationEvent> eventCaptor = ArgumentCaptor.forClass(AlarmNotificationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        AlarmNotificationEvent event = eventCaptor.getValue();
        assertThat(event.receiver().getId()).isEqualTo(MEMBER_ID);
        assertThat(event.type()).isEqualTo(AlarmType.ROADMAP);
        assertThat(event.content()).isEqualTo(RoadmapServiceImpl.ROADMAP_ALARM_CONTENT);
    }

    @Test
    @DisplayName("create - 회원이 존재하지 않으면 MemberHandler, AI 호출/저장 모두 미수행")
    void createThrowsWhenMemberNotFound() {
        when(memberRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roadmapService.create(999L, sampleRequest()))
                .isInstanceOf(MemberHandler.class);

        verifyNoInteractions(roadmapAiClient);
        verify(roadmapRepository, never()).save(any(Roadmap.class));
    }

    @Test
    @DisplayName("create - AI 응답이 비어 있으면 INVALID_CONTENT 예외, 저장 미수행")
    void createThrowsWhenAiContentBlank() {
        CreateRoadmapRequest req = sampleRequest();
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(author));
        when(roadmapAiClient.generate(req, MEMBER_ID)).thenReturn("   ");

        assertThatThrownBy(() -> roadmapService.create(MEMBER_ID, req))
                .isInstanceOf(RoadmapHandler.class);

        verify(roadmapRepository, never()).save(any(Roadmap.class));
    }

    @Test
    @DisplayName("getList - 본인이 생성한 로드맵을 카드 화면 경량 DTO 로 반환 (content JSON 의 roadmap_title 추출)")
    void getListReturnsMyRoadmaps() {
        when(roadmapRepository.findByMemberIdOrderByCreatedAtDesc(MEMBER_ID)).thenReturn(List.of(roadmap));

        List<RoadmapListResponse> result = roadmapService.getList(author);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(ROADMAP_ID);
        assertThat(result.get(0).title()).isEqualTo("백엔드 개발자 로드맵");
        assertThat(result.get(0).createdAt()).isNotNull();
    }

    @Test
    @DisplayName("getList - content JSON 이 망가져 title 추출 불가하면 폴백 \"로드맵\"")
    void getListFallbackTitleWhenContentBroken() {
        Roadmap broken = Roadmap.builder().member(author).content("not-a-json").build();
        ReflectionTestUtils.setField(broken, "id", 99L);
        when(roadmapRepository.findByMemberIdOrderByCreatedAtDesc(MEMBER_ID)).thenReturn(List.of(broken));

        List<RoadmapListResponse> result = roadmapService.getList(author);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("로드맵");
    }

    @Test
    @DisplayName("getOne - 본인 소유 로드맵을 반환한다")
    void getOneReturnsRoadmap() {
        when(roadmapRepository.findById(ROADMAP_ID)).thenReturn(Optional.of(roadmap));

        RoadmapResponse response = roadmapService.getOne(author, ROADMAP_ID);

        assertThat(response.id()).isEqualTo(ROADMAP_ID);
        assertThat(response.content()).isEqualTo(GENERATED_CONTENT);
    }

    @Test
    @DisplayName("getOne - 본인 소유가 아니면 ROADMAP_UNAUTHORIZED 예외")
    void getOneThrowsWhenUnauthorized() {
        when(roadmapRepository.findById(ROADMAP_ID)).thenReturn(Optional.of(roadmap));

        assertThatThrownBy(() -> roadmapService.getOne(other, ROADMAP_ID))
                .isInstanceOf(RoadmapHandler.class);
    }

    @Test
    @DisplayName("getOne - 존재하지 않으면 NOT_FOUND 예외")
    void getOneThrowsWhenNotFound() {
        when(roadmapRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roadmapService.getOne(author, 999L))
                .isInstanceOf(RoadmapHandler.class);
    }

    @Test
    @DisplayName("delete - 작성자가 정상 삭제하면 RoadmapAlarm 먼저 삭제 후 Roadmap 삭제, S3 객체도 함께 삭제")
    void deleteByAuthorSucceeds() {
        when(roadmapRepository.findById(ROADMAP_ID)).thenReturn(Optional.of(roadmap));

        roadmapService.delete(author, ROADMAP_ID);

        InOrder inOrder = inOrder(roadmapAlarmRepository, roadmapRepository);
        inOrder.verify(roadmapAlarmRepository).deleteByRoadmapId(ROADMAP_ID);
        inOrder.verify(roadmapRepository).delete(roadmap);

        // S3 객체도 best-effort 로 함께 삭제 (키 규약: roadmaps/{id}.pdf)
        verify(s3Service).deleteImage("roadmaps/" + ROADMAP_ID + ".pdf");
    }

    @Test
    @DisplayName("delete - S3 삭제 실패는 swallow (트랜잭션 영향 없음)")
    void deleteSwallowsS3Failure() {
        when(roadmapRepository.findById(ROADMAP_ID)).thenReturn(Optional.of(roadmap));
        doThrow(new RuntimeException("S3 down")).when(s3Service).deleteImage(any());

        // 예외가 전파되지 않아야 함
        roadmapService.delete(author, ROADMAP_ID);

        verify(roadmapAlarmRepository).deleteByRoadmapId(ROADMAP_ID);
        verify(roadmapRepository).delete(roadmap);
    }

    @Test
    @DisplayName("delete - 작성자가 아니면 UNAUTHORIZED 예외, 자식/본체 모두 미삭제")
    void deleteThrowsWhenUnauthorized() {
        when(roadmapRepository.findById(ROADMAP_ID)).thenReturn(Optional.of(roadmap));

        assertThatThrownBy(() -> roadmapService.delete(other, ROADMAP_ID))
                .isInstanceOf(RoadmapHandler.class);
        verify(roadmapAlarmRepository, never()).deleteByRoadmapId(any());
        verify(roadmapRepository, never()).delete(any());
    }

    @Test
    @DisplayName("delete - 존재하지 않으면 NOT_FOUND 예외")
    void deleteThrowsWhenNotFound() {
        when(roadmapRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roadmapService.delete(author, 999L))
                .isInstanceOf(RoadmapHandler.class);
    }

    @Test
    @DisplayName("countMyRoadmaps - 본인이 생성한 로드맵 수를 반환한다")
    void countMyRoadmapsReturnsCount() {
        when(roadmapRepository.countByMemberId(author.getId())).thenReturn(4L);

        long result = roadmapService.countMyRoadmaps(author);

        assertThat(result).isEqualTo(4L);
    }

    @Test
    @DisplayName("getDownloadUrl - 본인 소유 로드맵이면 presigned URL 발급 + 파일명에 roadmap_title 사용")
    void getDownloadUrlReturnsPresignedUrl() {
        when(roadmapRepository.findById(ROADMAP_ID)).thenReturn(Optional.of(roadmap));
        when(s3Service.generatePresignedUrl(
                eq("roadmaps/" + ROADMAP_ID + ".pdf"),
                eq(PRESIGN_TTL),
                eq("백엔드 개발자 로드맵.pdf")))
                .thenReturn("https://signed.example/key?X-Amz-Expires=300");

        RoadmapDownloadResponse response = roadmapService.getDownloadUrl(author, ROADMAP_ID);

        assertThat(response.fileUrl()).isEqualTo("https://signed.example/key?X-Amz-Expires=300");
        assertThat(response.fileName()).isEqualTo("백엔드 개발자 로드맵.pdf");
        assertThat(response.expiresAt()).isAfter(java.time.LocalDateTime.now());
    }

    @Test
    @DisplayName("getDownloadUrl - 본인 소유가 아니면 ROADMAP_UNAUTHORIZED (presigner 미호출)")
    void getDownloadUrlThrowsWhenUnauthorized() {
        when(roadmapRepository.findById(ROADMAP_ID)).thenReturn(Optional.of(roadmap));

        assertThatThrownBy(() -> roadmapService.getDownloadUrl(other, ROADMAP_ID))
                .isInstanceOf(RoadmapHandler.class);

        verify(s3Service, never()).generatePresignedUrl(any(), any(), any());
    }

    @Test
    @DisplayName("getDownloadUrl - 미존재면 ROADMAP_NOT_FOUND (presigner 미호출)")
    void getDownloadUrlThrowsWhenNotFound() {
        when(roadmapRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roadmapService.getDownloadUrl(author, 999L))
                .isInstanceOf(RoadmapHandler.class);

        verify(s3Service, never()).generatePresignedUrl(any(), any(), any());
    }

    @Test
    @DisplayName("getDownloadUrl - content JSON 이 망가져 title 추출 불가하면 폴백 파일명 'roadmap-{id}.pdf'")
    void getDownloadUrlFallsBackOnUnparseableContent() {
        Roadmap broken = Roadmap.builder().id(ROADMAP_ID).member(author).content("not-a-json").build();
        when(roadmapRepository.findById(ROADMAP_ID)).thenReturn(Optional.of(broken));
        when(s3Service.generatePresignedUrl(any(), any(), eq("roadmap-" + ROADMAP_ID + ".pdf")))
                .thenReturn("https://signed.example/fallback");

        RoadmapDownloadResponse response = roadmapService.getDownloadUrl(author, ROADMAP_ID);

        assertThat(response.fileName()).isEqualTo("roadmap-" + ROADMAP_ID + ".pdf");
    }

    @Test
    @DisplayName("getDownloadUrl - 파일명의 OS 금지 문자(/:*?\"<>|)는 sanitize 된다")
    void getDownloadUrlSanitizesFilename() {
        String unsafe = "{\"roadmap_title\":\"AI/ML <팀장> 로드맵: 핵심\"}";
        Roadmap r = Roadmap.builder().id(ROADMAP_ID).member(author).content(unsafe).build();
        when(roadmapRepository.findById(ROADMAP_ID)).thenReturn(Optional.of(r));
        when(s3Service.generatePresignedUrl(any(), any(), any()))
                .thenReturn("https://signed.example/x");

        RoadmapDownloadResponse response = roadmapService.getDownloadUrl(author, ROADMAP_ID);

        assertThat(response.fileName()).isEqualTo("AIML 팀장 로드맵 핵심.pdf");
    }
}
