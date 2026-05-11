package mju.capstone.ddingconnect.domain.interested_job.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import mju.capstone.ddingconnect.domain.interested_job.dto.request.CreateTargetJobRequest;
import mju.capstone.ddingconnect.domain.interested_job.dto.request.UpdateTargetJobRequest;
import mju.capstone.ddingconnect.domain.interested_job.dto.response.TargetJobResponse;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.global.auth.annotation.LoginMember;
import mju.capstone.ddingconnect.global.response.exception.handler.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "관심 직군", description = "관심 직군(TargetJob) 컨트롤러 — 추가/목록 조회/수정/삭제 CRUD 엔드포인트를 제공합니다.")
public interface TargetJobSwagger {

    @Operation(
            summary = "관심 직군 추가",
            description = "로그인된 회원의 관심 직군을 추가합니다."
    )
    @PostMapping
    ApiResponse<TargetJobResponse> addTargetJob(
            @Parameter(hidden = true) @LoginMember Member member,
            @Parameter(description = "관심 직군 추가 정보 (직군명 등)")
            @RequestBody CreateTargetJobRequest request);




    @Operation(
            summary = "내 관심 직군 목록 조회",
            description = "로그인된 회원이 등록한 관심 직군 목록을 조회합니다."
    )
    @GetMapping
    ApiResponse<List<TargetJobResponse>> getMyTargetJobs(
            @Parameter(hidden = true) @LoginMember Member member);




    @Operation(
            summary = "관심 직군 수정",
            description = "본인이 등록한 관심 직군의 정보를 수정합니다."
    )
    @PatchMapping("/{targetJobId}")
    ApiResponse<TargetJobResponse> updateTargetJob(
            @Parameter(hidden = true) @LoginMember Member member,
            @Parameter(description = "수정할 관심 직군 ID")
            @PathVariable Long targetJobId,
            @Parameter(description = "관심 직군 수정 정보")
            @RequestBody UpdateTargetJobRequest request);




    @Operation(
            summary = "관심 직군 삭제",
            description = "본인이 등록한 관심 직군을 삭제합니다."
    )
    @DeleteMapping("/{targetJobId}")
    ApiResponse<String> deleteTargetJob(
            @Parameter(hidden = true) @LoginMember Member member,
            @Parameter(description = "삭제할 관심 직군 ID")
            @PathVariable Long targetJobId);

}
