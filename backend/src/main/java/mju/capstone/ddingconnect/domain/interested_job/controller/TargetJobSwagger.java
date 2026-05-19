package mju.capstone.ddingconnect.domain.interested_job.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import mju.capstone.ddingconnect.domain.interested_job.dto.request.ReplaceTargetJobRequest;
import mju.capstone.ddingconnect.domain.interested_job.dto.response.TargetJobResponse;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.global.auth.annotation.LoginMember;
import mju.capstone.ddingconnect.global.response.exception.handler.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "관심 직군", description = "관심 직군(TargetJob) 컨트롤러 — 일괄 교체(REPLACE)/목록 조회 엔드포인트를 제공합니다.")
public interface TargetJobSwagger {

    @Operation(
            summary = "관심 직군 일괄 교체",
            description = "로그인된 회원의 관심 직군 전체를 요청 리스트로 교체(REPLACE)합니다. "
                    + "빈 리스트는 전부 삭제를 의미하며, 리스트 내부 중복은 서버가 제거합니다. categories 가 null 이면 400 입니다."
    )
    @PatchMapping
    ApiResponse<List<TargetJobResponse>> replaceTargetJobs(
            @Parameter(hidden = true) @LoginMember Member member,
            @Parameter(description = "교체할 관심 직군 전체 리스트")
            @RequestBody ReplaceTargetJobRequest request);


    @Operation(
            summary = "내 관심 직군 목록 조회",
            description = "로그인된 회원이 등록한 관심 직군 목록을 조회합니다."
    )
    @GetMapping
    ApiResponse<List<TargetJobResponse>> getMyTargetJobs(
            @Parameter(hidden = true) @LoginMember Member member);
}
