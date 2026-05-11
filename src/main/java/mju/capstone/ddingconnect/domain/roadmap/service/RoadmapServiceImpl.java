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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoadmapServiceImpl implements RoadmapService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RoadmapRepository roadmapRepository;
    private final RoadmapAlarmRepository roadmapAlarmRepository;

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
                .content("로드맵 생성이 완료되었습니다.")
                .isRead(false)
                .build());

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

        roadmapRepository.delete(roadmap);
    }
}
