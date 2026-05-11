package mju.capstone.ddingconnect.domain.roadmap.service;

import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.roadmap.dto.request.CreateRoadmapRequest;
import mju.capstone.ddingconnect.domain.roadmap.dto.response.RoadmapResponse;

import java.util.List;

public interface RoadmapService {

    RoadmapResponse create(Member member, CreateRoadmapRequest request);

    List<RoadmapResponse> getList();

    RoadmapResponse getOne(Long roadmapId);

    void delete(Member member, Long roadmapId);
}
