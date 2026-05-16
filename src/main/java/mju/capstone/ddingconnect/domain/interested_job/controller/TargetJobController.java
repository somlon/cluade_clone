package mju.capstone.ddingconnect.domain.interested_job.controller;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import mju.capstone.ddingconnect.domain.interested_job.dto.request.CreateTargetJobRequest;
import mju.capstone.ddingconnect.domain.interested_job.dto.request.UpdateTargetJobRequest;
import mju.capstone.ddingconnect.domain.interested_job.dto.response.TargetJobResponse;
import mju.capstone.ddingconnect.domain.interested_job.service.TargetJobService;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.global.auth.annotation.LoginMember;
import mju.capstone.ddingconnect.global.common.SuccessMessage;
import mju.capstone.ddingconnect.global.response.exception.handler.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * [관심 직군 컨트롤러]
 * TargetJob 엔티티에 대한 CRUD API 엔드포인트
 */
@RestController
@RequestMapping("/api/v1/target-jobs")
@RequiredArgsConstructor
public class TargetJobController implements TargetJobSwagger {

    private final TargetJobService targetJobService;

    /** 관심 직군 추가 (Create) */
    @PostMapping
    public ApiResponse<TargetJobResponse> addTargetJob(
            @Parameter(hidden = true) @LoginMember Member member,
            @RequestBody CreateTargetJobRequest request) {
        return ApiResponse.onSuccess(targetJobService.create(member, request));
    }

    /** 내 관심 직군 목록 조회 (Read) */
    @GetMapping
    public ApiResponse<List<TargetJobResponse>> getMyTargetJobs(
            @Parameter(hidden = true) @LoginMember Member member) {
        return ApiResponse.onSuccess(targetJobService.getMyTargetJobs(member));
    }

    /** 관심 직군 수정 (Update) */
    @PatchMapping("/{targetJobId}")
    public ApiResponse<TargetJobResponse> updateTargetJob(
            @Parameter(hidden = true) @LoginMember Member member,
            @PathVariable Long targetJobId,
            @RequestBody UpdateTargetJobRequest request) {
        return ApiResponse.onSuccess(targetJobService.update(member, targetJobId, request));
    }

    /** 관심 직군 삭제 (Delete) */
    @DeleteMapping("/{targetJobId}")
    public ApiResponse<String> deleteTargetJob(
            @Parameter(hidden = true) @LoginMember Member member,
            @PathVariable Long targetJobId) {
        targetJobService.delete(member, targetJobId);
        return ApiResponse.onSuccess(SuccessMessage.TARGET_JOB_DELETED);
    }
}
