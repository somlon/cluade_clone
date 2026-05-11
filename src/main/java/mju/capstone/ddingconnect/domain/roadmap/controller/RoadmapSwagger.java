package mju.capstone.ddingconnect.domain.roadmap.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.roadmap.dto.request.CreateRoadmapRequest;
import mju.capstone.ddingconnect.domain.roadmap.dto.response.RoadmapResponse;
import mju.capstone.ddingconnect.global.auth.annotation.LoginMember;
import mju.capstone.ddingconnect.global.response.exception.handler.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "로드맵", description = "AI 로드맵 컨트롤러 — 등록/목록 조회/상세 조회/삭제 엔드포인트를 제공합니다. (수정은 미지원)")
public interface RoadmapSwagger {

    @Operation(
            summary = "로드맵 등록",
            description = "AI가 생성한 로드맵을 등록합니다. 로그인된 회원만 호출할 수 있습니다."
    )
    @PostMapping
    ApiResponse<RoadmapResponse> createRoadmap(
            @Parameter(hidden = true) @LoginMember Member member,
            @Parameter(description = "로드맵 등록 정보 (AI가 생성한 JSON 형식의 content)")
            @RequestBody CreateRoadmapRequest request);




    @Operation(
            summary = "로드맵 목록 조회",
            description = "등록된 모든 로드맵 목록을 조회합니다."
    )
    @GetMapping
    ApiResponse<List<RoadmapResponse>> getRoadmaps();




    @Operation(
            summary = "로드맵 상세 조회",
            description = "로드맵 카드를 클릭했을 때 AI가 생성한 JSON 로드맵 전체 내용을 반환합니다."
    )
    @GetMapping("/{roadmapId}")
    ApiResponse<RoadmapResponse> getRoadmap(
            @Parameter(description = "조회할 로드맵 ID")
            @PathVariable Long roadmapId);




    @Operation(
            summary = "로드맵 삭제",
            description = "본인이 등록한 로드맵을 삭제합니다."
    )
    @DeleteMapping("/{roadmapId}")
    ApiResponse<String> deleteRoadmap(
            @Parameter(hidden = true) @LoginMember Member member,
            @Parameter(description = "삭제할 로드맵 ID")
            @PathVariable Long roadmapId);

}
