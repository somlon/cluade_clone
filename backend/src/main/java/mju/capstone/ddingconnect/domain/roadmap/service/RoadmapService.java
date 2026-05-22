package mju.capstone.ddingconnect.domain.roadmap.service;

import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.roadmap.dto.request.CreateRoadmapRequest;
import mju.capstone.ddingconnect.domain.roadmap.dto.response.RoadmapResponse;

import java.util.List;

public interface RoadmapService {

    /**
     * 입력 폼 6필드로 데이터 파트 AI 를 호출해 로드맵을 생성·저장한다.
     *
     * @param memberId 로드맵을 생성할 회원 ID (URL 로 전달)
     * @param request  로드맵 입력 폼 6필드
     */
    RoadmapResponse create(Long memberId, CreateRoadmapRequest request);

    /** 로그인 회원이 생성한 로드맵 목록을 최신순으로 조회한다. */
    List<RoadmapResponse> getList(Member member);

    RoadmapResponse getOne(Long roadmapId);

    void delete(Member member, Long roadmapId);

    /** 본인이 생성한 로드맵 수 */
    long countMyRoadmaps(Member member);
}
