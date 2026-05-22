package mju.capstone.ddingconnect.domain.roadmap.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
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
            description = "입력 폼 6필드와 회원 ID(memberId)를 받아 데이터 파트 AI 로 로드맵을 생성·저장합니다. " +
                    "응답으로 받은 로드맵 ID 로 상세 조회(GET /api/v1/roadmaps/{roadmapId})를 호출하면 생성 결과를 볼 수 있습니다."
    )
    @PostMapping
    ApiResponse<RoadmapResponse> createRoadmap(
            @Parameter(description = "로드맵을 생성할 회원 ID", required = true)
            @RequestParam Long memberId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "로드맵 생성 입력 폼 (6필드)",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CreateRoadmapRequest.class),
                            examples = @ExampleObject(
                                    name = "로드맵 생성 입력 예시",
                                    summary = "입력 폼 6필드",
                                    value = """
                                            {
                                              "grade": 3,
                                              "gpa": 4.0,
                                              "major": "응용소프트웨어학과",
                                              "targetJob": "BACKEND",
                                              "currentSkills": ["JAVA", "SPRING"],
                                              "targetCompany": "카카오"
                                            }
                                            """
                            )
                    )
            )
            @RequestBody CreateRoadmapRequest request);




    @Operation(
            summary = "로드맵 목록 조회",
            description = "등록된 모든 로드맵 목록을 조회합니다."
    )
    @GetMapping
    ApiResponse<List<RoadmapResponse>> getRoadmaps();




    @Operation(
            summary = "로드맵 상세 조회",
            description = "로드맵 ID 로 저장된 로드맵을 조회합니다. content 는 데이터 파트 AI 가 생성한 로드맵 JSON 문자열로, 저장된 값을 그대로 반환합니다."
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
