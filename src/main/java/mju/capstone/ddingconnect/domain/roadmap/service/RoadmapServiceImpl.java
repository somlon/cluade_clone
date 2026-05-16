package mju.capstone.ddingconnect.domain.roadmap.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.roadmap.domain.Roadmap;
import mju.capstone.ddingconnect.domain.roadmap.domain.RoadmapAlarm;
import mju.capstone.ddingconnect.domain.roadmap.domain.repository.RoadmapAlarmRepository;
import mju.capstone.ddingconnect.domain.roadmap.domain.repository.RoadmapRepository;
import mju.capstone.ddingconnect.domain.roadmap.dto.request.CreateRoadmapRequest;
import mju.capstone.ddingconnect.domain.roadmap.dto.response.RoadmapResponse;
import mju.capstone.ddingconnect.global.response.code.status.ErrorStatus;
import mju.capstone.ddingconnect.global.response.exception.handler.RoadmapHandler;
import mju.capstone.ddingconnect.global.sse.AlarmNotificationEvent;
import mju.capstone.ddingconnect.global.alarm.AlarmType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoadmapServiceImpl implements RoadmapService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    // 테스트도 동일 상수를 참조하므로 package-private 노출
    static final String ROADMAP_ALARM_CONTENT = "로드맵 생성이 완료되었습니다.";

    private final RoadmapRepository roadmapRepository;
    private final RoadmapAlarmRepository roadmapAlarmRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public RoadmapResponse create(Member member, CreateRoadmapRequest request) {
        validateJsonContent(request.content());

        Roadmap roadmap = Roadmap.builder()
                .member(member)
                .content(request.content())
                .build();
        Roadmap saved = roadmapRepository.save(roadmap);

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

    private void validateJsonContent(String content) {
        if (content == null || content.isBlank()) {
            throw new RoadmapHandler(ErrorStatus.ROADMAP_INVALID_CONTENT);
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(content);
            if (!node.isObject() && !node.isArray()) {
                throw new RoadmapHandler(ErrorStatus.ROADMAP_INVALID_CONTENT);
            }
        } catch (JsonProcessingException e) {
            throw new RoadmapHandler(ErrorStatus.ROADMAP_INVALID_CONTENT);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoadmapResponse> getList() {
        return roadmapRepository.findAll()
                .stream().map(RoadmapResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RoadmapResponse getOne(Long roadmapId) {
        Roadmap roadmap = roadmapRepository.findById(roadmapId)
                .orElseThrow(() -> new RoadmapHandler(ErrorStatus.ROADMAP_NOT_FOUND));
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
    }
}
