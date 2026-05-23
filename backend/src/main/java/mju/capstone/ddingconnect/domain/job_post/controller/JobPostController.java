package mju.capstone.ddingconnect.domain.job_post.controller;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mju.capstone.ddingconnect.domain.job_post.dto.request.CreateJobPostLinkRequest;
import mju.capstone.ddingconnect.domain.job_post.dto.request.CreateJobPostRequest;
import mju.capstone.ddingconnect.domain.job_post.dto.request.UpdateJobPostRequest;
import mju.capstone.ddingconnect.domain.job_post.dto.response.GraduatePostResponse;
import mju.capstone.ddingconnect.domain.job_post.dto.response.JobPostResponse;
import mju.capstone.ddingconnect.domain.job_post.service.JobPostService;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.global.auth.annotation.LoginMember;
import mju.capstone.ddingconnect.global.common.SuccessMessage;
import mju.capstone.ddingconnect.global.response.exception.handler.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/job-post")
@RequiredArgsConstructor
public class JobPostController implements JobPostSwagger {

    private final JobPostService jobPostService;

    /** 구직 공고 등록 (Create - 졸업생 전용) */
    @PostMapping
    public ApiResponse<JobPostResponse> createJobPost(
            @Parameter(hidden = true) @LoginMember Member member,
            @RequestBody CreateJobPostRequest request) {
        return ApiResponse.onSuccess(jobPostService.create(member, request));
    }

    /** 구직 공고 링크 전용 등록 (Create - 졸업생 전용) — '나의 공고 올리기' 모달 흐름 */
    @PostMapping("/link")
    public ApiResponse<JobPostResponse> createJobPostLink(
            @Parameter(hidden = true) @LoginMember Member member,
            @Valid @RequestBody CreateJobPostLinkRequest request) {
        return ApiResponse.onSuccess(jobPostService.createFromLink(member, request));
    }

    /** 구직 공고 목록 조회 (Read) — 호환성 유지 (선배/일반 구분 없이 전체) */
    @GetMapping
    public ApiResponse<List<JobPostResponse>> getJobPosts() {
        return ApiResponse.onSuccess(jobPostService.getList());
    }

    /** 선배가 올린 공고 목록 (Read) — 구직 정보 화면 상단 섹션 */
    @GetMapping("/graduates")
    public ApiResponse<List<GraduatePostResponse>> getGraduatePosts() {
        return ApiResponse.onSuccess(jobPostService.getGraduatePosts());
    }

    /** 일반(크롤링) 공고 목록 (Read) — 구직 정보 화면 하단 섹션 */
    @GetMapping("/crawled")
    public ApiResponse<List<JobPostResponse>> getCrawledJobPosts() {
        return ApiResponse.onSuccess(jobPostService.getCrawledPosts());
    }

    /** 구직 공고 상세 조회 (Read - 공고 카드를 클릭했을 때 회사명/직무/경력/마감일/위치 등 전체 정보 반환) */
    @GetMapping("/{jobPostId}")
    public ApiResponse<JobPostResponse> getJobPost(@PathVariable Long jobPostId) {
        return ApiResponse.onSuccess(jobPostService.getOne(jobPostId));
    }

    /** 구직 공고 수정 (Update) */
    @PatchMapping("/{jobPostId}")
    public ApiResponse<JobPostResponse> updateJobPost(
            @Parameter(hidden = true) @LoginMember Member member,
            @PathVariable Long jobPostId,
            @RequestBody UpdateJobPostRequest request) {
        return ApiResponse.onSuccess(jobPostService.update(member, jobPostId, request));
    }

    /** 구직 공고 삭제 (Delete) */
    @DeleteMapping("/{jobPostId}")
    public ApiResponse<String> deleteJobPost(
            @Parameter(hidden = true) @LoginMember Member member,
            @PathVariable Long jobPostId) {
        jobPostService.delete(member, jobPostId);
        return ApiResponse.onSuccess(SuccessMessage.JOB_POST_DELETED);
    }
}
