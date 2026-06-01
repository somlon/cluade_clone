package mju.capstone.ddingconnect.domain.roadmap.controller;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.member.domain.MemberRole;
import mju.capstone.ddingconnect.domain.roadmap.dto.request.CreateRoadmapRequest;
import mju.capstone.ddingconnect.domain.roadmap.dto.response.RoadmapDownloadResponse;
import mju.capstone.ddingconnect.domain.roadmap.dto.response.RoadmapListResponse;
import mju.capstone.ddingconnect.domain.roadmap.dto.response.RoadmapResponse;
import mju.capstone.ddingconnect.domain.roadmap.service.RoadmapService;
import mju.capstone.ddingconnect.global.auth.annotation.LoginMember;
import mju.capstone.ddingconnect.global.common.SuccessMessage;
import mju.capstone.ddingconnect.global.response.code.status.ErrorStatus;
import mju.capstone.ddingconnect.global.response.exception.handler.ApiResponse;
import mju.capstone.ddingconnect.global.response.exception.handler.MemberHandler;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roadmaps")
@RequiredArgsConstructor
public class RoadmapController implements RoadmapSwagger {

    private final RoadmapService roadmapService;

    /** 로드맵 생성 (Create) — 로그인 회원과 입력 폼 6필드로 데이터 파트 AI 호출 후 저장.
     *  응답으로 "생성된 로드맵" 카드 목록(방금 만든 항목 포함, 최신순)을 그대로 반환해 프론트가 단일 호출로 화면을 렌더한다. */
    @PostMapping
    public ApiResponse<List<RoadmapListResponse>> createRoadmap(
            @Parameter(hidden = true) @LoginMember Member member,
            @RequestBody CreateRoadmapRequest request) {
        return ApiResponse.onSuccess(roadmapService.create(member.getId(), request));
    }

    /** 로드맵 목록 조회 (Read) — 로그인 회원이 생성한 로드맵만 최신순 반환. GRADUATE 는 차단. */
    @GetMapping
    public ApiResponse<List<RoadmapListResponse>> getRoadmaps(
            @Parameter(hidden = true) @LoginMember Member member) {
        // 졸업생 마이페이지/나의 활동엔 로드맵 탭 자체가 없다. 프론트가 호출하지 않더라도 서버 일관성을 위해 차단.
        if (member.getRole() == MemberRole.GRADUATE) {
            throw new MemberHandler(ErrorStatus.MEMBER_FIELD_ROLE_MISMATCH);
        }
        return ApiResponse.onSuccess(roadmapService.getList(member));
    }

    /** 로드맵 상세 조회 (Read - 본인 소유 로드맵만, AI 생성 JSON 전체 반환) */
    @GetMapping("/{roadmapId}")
    public ApiResponse<RoadmapResponse> getRoadmap(
            @LoginMember Member member,
            @PathVariable Long roadmapId) {
        return ApiResponse.onSuccess(roadmapService.getOne(member, roadmapId));
    }

    /** 로드맵 삭제 (Delete) */
    @DeleteMapping("/{roadmapId}")
    public ApiResponse<String> deleteRoadmap(
            @Parameter(hidden = true) @LoginMember Member member,
            @PathVariable Long roadmapId) {
        roadmapService.delete(member, roadmapId);
        return ApiResponse.onSuccess(SuccessMessage.ROADMAP_DELETED);
    }

    /** 로드맵 PDF 다운로드 URL 발급 — 본인 소유 로드맵에 대해 짧은 만료의 S3 presigned URL 반환 */
    @GetMapping("/{roadmapId}/download")
    public ApiResponse<RoadmapDownloadResponse> downloadRoadmap(
            @Parameter(hidden = true) @LoginMember Member member,
            @PathVariable Long roadmapId) {
        return ApiResponse.onSuccess(roadmapService.getDownloadUrl(member, roadmapId));
    }
}
