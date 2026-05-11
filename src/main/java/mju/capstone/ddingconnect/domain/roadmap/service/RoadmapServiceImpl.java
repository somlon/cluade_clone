package mju.capstone.ddingconnect.domain.roadmap.service;

import lombok.RequiredArgsConstructor;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.roadmap.domain.Roadmap;
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

    private final RoadmapRepository roadmapRepository;

    @Override
    @Transactional
    public RoadmapResponse create(Member member, CreateRoadmapRequest request) {
        Roadmap roadmap = Roadmap.builder()
                .member(member)
                .content(request.content())
                .build();
        return RoadmapResponse.from(roadmapRepository.save(roadmap));
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
