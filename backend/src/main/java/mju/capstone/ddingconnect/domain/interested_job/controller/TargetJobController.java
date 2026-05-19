package mju.capstone.ddingconnect.domain.interested_job.controller;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import mju.capstone.ddingconnect.domain.interested_job.dto.request.ReplaceTargetJobRequest;
import mju.capstone.ddingconnect.domain.interested_job.dto.response.TargetJobResponse;
import mju.capstone.ddingconnect.domain.interested_job.service.TargetJobService;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.global.auth.annotation.LoginMember;
import mju.capstone.ddingconnect.global.response.exception.handler.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * [관심 직군 컨트롤러]
 * 마이페이지 수정 화면이 유일한 진입점이라, 단건 CRUD 대신
 * "수정 완료" 시점의 일괄 교체(REPLACE) 와 목록 조회만 제공한다.
 */
@RestController
@RequestMapping("/api/v1/target-jobs")
@RequiredArgsConstructor
public class TargetJobController implements TargetJobSwagger {

    private final TargetJobService targetJobService;

    /** 관심 직군 일괄 교체 (REPLACE) */
    @PatchMapping
    public ApiResponse<List<TargetJobResponse>> replaceTargetJobs(
            @Parameter(hidden = true) @LoginMember Member member,
            @RequestBody ReplaceTargetJobRequest request) {
        return ApiResponse.onSuccess(targetJobService.replace(member, request));
    }

    /** 내 관심 직군 목록 조회 (Read) */
    @GetMapping
    public ApiResponse<List<TargetJobResponse>> getMyTargetJobs(
            @Parameter(hidden = true) @LoginMember Member member) {
        return ApiResponse.onSuccess(targetJobService.getMyTargetJobs(member));
    }
}
