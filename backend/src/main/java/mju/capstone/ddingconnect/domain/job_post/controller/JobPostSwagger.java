package mju.capstone.ddingconnect.domain.job_post.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import mju.capstone.ddingconnect.domain.job_post.dto.request.CreateJobPostLinkRequest;
import mju.capstone.ddingconnect.domain.job_post.dto.request.CreateJobPostRequest;
import mju.capstone.ddingconnect.domain.job_post.dto.request.UpdateJobPostRequest;
import mju.capstone.ddingconnect.domain.job_post.dto.response.GraduatePostResponse;
import mju.capstone.ddingconnect.domain.job_post.dto.response.JobPostResponse;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.global.auth.annotation.LoginMember;
import mju.capstone.ddingconnect.global.response.exception.handler.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "구직 공고", description = "구직 공고 컨트롤러 — 등록/조회/수정/삭제 CRUD 엔드포인트를 제공합니다. (졸업생만 등록 가능)")
public interface JobPostSwagger {

    @Operation(
            summary = "구직 공고 등록",
            description = "구직 공고를 등록합니다. 졸업생 전용 API이며, 로그인된 회원만 호출할 수 있습니다."
    )
    @PostMapping
    ApiResponse<JobPostResponse> createJobPost(
            @Parameter(hidden = true) @LoginMember Member member,
            @Parameter(description = "구직 공고 등록 정보 (회사명, 직무, 경력, 마감일, 위치, 선호 언어 목록 등). preferredLanguages 는 문자열 배열로 여러 개 입력 가능")
            @RequestBody CreateJobPostRequest request);




    @Operation(
            summary = "구직 공고 링크 전용 등록 (졸업생 전용)",
            description = "졸업생이 '나의 공고 올리기' 모달에서 detailUrl 하나만 입력해 공고를 등록합니다. "
                    + "본문 11필드(회사명/직무/마감일 등)는 채우지 않으며 jobType 이 null 이라 알람 분기는 스킵됩니다. "
                    + "STUDENT/UNKNOWN 은 403(`POST_CONTENTS_NOT_GRADUATE`)으로 거부됩니다."
    )
    @PostMapping("/link")
    ApiResponse<JobPostResponse> createJobPostLink(
            @Parameter(hidden = true) @LoginMember Member member,
            @Parameter(description = "공고 링크 (http(s) URL)")
            @RequestBody CreateJobPostLinkRequest request);




    @Operation(
            summary = "구직 공고 목록 조회 (전체)",
            description = "등록된 모든 구직 공고 목록을 조회합니다. 선배/일반 구분 없이 전체를 반환하므로 호환성 용도로 유지됩니다. "
                    + "신규 화면은 선배(`/graduates`)·일반(`/crawled`) 분리 엔드포인트를 사용하세요."
    )
    @GetMapping
    ApiResponse<List<JobPostResponse>> getJobPosts();




    @Operation(
            summary = "선배 공고 목록 조회",
            description = "졸업생이 직접 등록한 공고만(GraduateJobPost 매핑 존재) 카드 응답으로 반환합니다. "
                    + "응답 DTO 는 일반 공고 본문 + 등록자(닉네임/학과/직무/경력) 정보를 포함합니다. "
                    + "구직 정보 화면의 '선배가 올린 공고' 섹션용입니다."
    )
    @GetMapping("/graduates")
    ApiResponse<List<GraduatePostResponse>> getGraduatePosts();




    @Operation(
            summary = "일반(크롤링) 공고 목록 조회",
            description = "선배 매핑이 없는 PostContents (크롤링 적재 등) 만 반환합니다. "
                    + "응답 DTO 는 기존 JobPostResponse 를 재사용합니다. "
                    + "구직 정보 화면의 '공고 목록' 섹션용입니다."
    )
    @GetMapping("/crawled")
    ApiResponse<List<JobPostResponse>> getCrawledJobPosts();




    @Operation(
            summary = "구직 공고 상세 조회",
            description = "구직 공고 카드를 클릭했을 때 회사명/직무/경력/마감일/위치 등 전체 정보를 반환합니다."
    )
    @GetMapping("/{jobPostId}")
    ApiResponse<JobPostResponse> getJobPost(
            @Parameter(description = "조회할 구직 공고 ID")
            @PathVariable Long jobPostId);




    @Operation(
            summary = "구직 공고 수정",
            description = "본인이 작성한 구직 공고의 일부 또는 전체 정보를 수정합니다."
    )
    @PatchMapping("/{jobPostId}")
    ApiResponse<JobPostResponse> updateJobPost(
            @Parameter(hidden = true) @LoginMember Member member,
            @Parameter(description = "수정할 구직 공고 ID")
            @PathVariable Long jobPostId,
            @Parameter(description = "구직 공고 수정 정보")
            @RequestBody UpdateJobPostRequest request);




    @Operation(
            summary = "구직 공고 삭제",
            description = "본인이 작성한 구직 공고를 삭제합니다."
    )
    @DeleteMapping("/{jobPostId}")
    ApiResponse<String> deleteJobPost(
            @Parameter(hidden = true) @LoginMember Member member,
            @Parameter(description = "삭제할 구직 공고 ID")
            @PathVariable Long jobPostId);

}
