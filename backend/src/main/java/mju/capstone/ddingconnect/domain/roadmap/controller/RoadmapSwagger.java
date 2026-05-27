package mju.capstone.ddingconnect.domain.roadmap.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.roadmap.dto.request.CreateRoadmapRequest;
import mju.capstone.ddingconnect.domain.roadmap.dto.response.RoadmapDownloadResponse;
import mju.capstone.ddingconnect.domain.roadmap.dto.response.RoadmapListResponse;
import mju.capstone.ddingconnect.domain.roadmap.dto.response.RoadmapResponse;
import mju.capstone.ddingconnect.global.auth.annotation.LoginMember;
import mju.capstone.ddingconnect.global.response.exception.handler.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "로드맵", description = "AI 로드맵 컨트롤러 — 등록/목록 조회/상세 조회/삭제 엔드포인트를 제공합니다. (수정은 미지원)")
public interface RoadmapSwagger {

    @Operation(
            summary = "로드맵 생성",
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
            summary = "내 로드맵 목록 조회",
            description = "로그인한 회원이 생성한 로드맵 목록을 최신순(createdAt 내림차순)으로 조회합니다.\n\n" +
                    "카드 화면 전용 경량 응답 — `id` / `title` / `createdAt` 3 필드만 반환합니다.\n" +
                    "`title` 은 저장된 `content` JSON 에서 `roadmap_title` 만 추출한 값이며, 추출 실패·빈 제목 시 `\"로드맵\"` 폴백.\n" +
                    "전체 본문(`content`)이 필요하면 단건 조회(`GET /api/v1/roadmaps/{roadmapId}`)를 호출하세요."
    )
    @Tag(name = "나의 활동")
    @GetMapping
    ApiResponse<List<RoadmapListResponse>> getRoadmaps(
            @Parameter(hidden = true) @LoginMember Member member);




    @Operation(
            summary = "로드맵 상세 조회",
            description = "본인이 생성한 로드맵을 ID 로 조회합니다. content 는 데이터 파트 AI 가 생성한 로드맵 JSON 문자열로, 저장된 값을 그대로 반환합니다.\n\n" +
                    "본인 소유 X → 403 `ROADMAP_UNAUTHORIZED`, 미존재 → 404 `ROADMAP_NOT_FOUND`.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ROADMAP_UNAUTHORIZED — 본인 소유 로드맵이 아님"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "ROADMAP_NOT_FOUND — 존재하지 않는 로드맵")
            }
    )
    @GetMapping("/{roadmapId}")
    ApiResponse<RoadmapResponse> getRoadmap(
            @Parameter(hidden = true) @LoginMember Member member,
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




    @Operation(
            summary = "로드맵 PDF 다운로드 URL 발급",
            description = "본인이 생성한 로드맵의 PDF 다운로드 URL 을 발급합니다. " +
                    "응답의 `fileUrl` 은 S3 presigned GET URL (만료 짧음, 기본 5분) 이며, " +
                    "프론트는 이 URL 을 `<a href download>` 또는 `window.location.href` 로 호출해 PDF 를 받아갑니다.\n\n" +
                    "본인 소유 X → 403 `ROADMAP_UNAUTHORIZED`, 미존재 → 404 `ROADMAP_NOT_FOUND`.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "다운로드 URL 발급 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = RoadmapDownloadResponse.class),
                                    examples = @ExampleObject(
                                            name = "성공 예시",
                                            value = """
                                                    {
                                                      "isSuccess": true,
                                                      "code": "COMMON200",
                                                      "message": "성공입니다.",
                                                      "result": {
                                                        "fileUrl": "https://<bucket>.s3.<region>.amazonaws.com/roadmaps/123.pdf?X-Amz-Algorithm=...&X-Amz-Expires=300&...",
                                                        "fileName": "백엔드 개발자 로드맵.pdf",
                                                        "expiresAt": "2026-05-27T10:35:00"
                                                      }
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "403",
                            description = "ROADMAP_UNAUTHORIZED — 본인 소유 로드맵이 아님"
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404",
                            description = "ROADMAP_NOT_FOUND — 존재하지 않는 로드맵"
                    )
            }
    )
    @GetMapping("/{roadmapId}/download")
    ApiResponse<RoadmapDownloadResponse> downloadRoadmap(
            @Parameter(hidden = true) @LoginMember Member member,
            @Parameter(description = "다운로드할 로드맵 ID")
            @PathVariable Long roadmapId);

}
