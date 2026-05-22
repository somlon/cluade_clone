package mju.capstone.ddingconnect.domain.roadmap.controller;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.roadmap.dto.request.CreateRoadmapRequest;
import mju.capstone.ddingconnect.domain.roadmap.dto.response.RoadmapResponse;
import mju.capstone.ddingconnect.domain.roadmap.service.RoadmapService;
import mju.capstone.ddingconnect.global.auth.annotation.LoginMember;
import mju.capstone.ddingconnect.global.common.SuccessMessage;
import mju.capstone.ddingconnect.global.response.exception.handler.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roadmaps")
@RequiredArgsConstructor
public class RoadmapController implements RoadmapSwagger {

    private final RoadmapService roadmapService;

    /** 로드맵 등록 (Create) — memberId 와 입력 폼 6필드로 데이터 파트 AI 호출 후 저장 */
    @PostMapping
    public ApiResponse<RoadmapResponse> createRoadmap(
            @RequestParam Long memberId,
            @RequestBody CreateRoadmapRequest request) {
        return ApiResponse.onSuccess(roadmapService.create(memberId, request));
    }

    /** 로드맵 목록 조회 (Read) */
    @GetMapping
    public ApiResponse<List<RoadmapResponse>> getRoadmaps() {
        return ApiResponse.onSuccess(roadmapService.getList());
    }

    /** 로드맵 상세 조회 (Read - 로드맵 카드를 클릭했을 때 AI가 생성한 JSON 로드맵 전체 내용 반환) */
    @GetMapping("/{roadmapId}")
    public ApiResponse<RoadmapResponse> getRoadmap(@PathVariable Long roadmapId) {
        return ApiResponse.onSuccess(roadmapService.getOne(roadmapId));
    }

    /** 로드맵 삭제 (Delete) */
    @DeleteMapping("/{roadmapId}")
    public ApiResponse<String> deleteRoadmap(
            @Parameter(hidden = true) @LoginMember Member member,
            @PathVariable Long roadmapId) {
        roadmapService.delete(member, roadmapId);
        return ApiResponse.onSuccess(SuccessMessage.ROADMAP_DELETED);
    }
}
