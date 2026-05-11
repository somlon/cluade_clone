package mju.capstone.ddingconnect.domain.techstack.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.techstack.dto.request.CreateTechStackRequest;
import mju.capstone.ddingconnect.domain.techstack.dto.response.TechStackResponse;
import mju.capstone.ddingconnect.global.auth.annotation.LoginMember;
import mju.capstone.ddingconnect.global.response.exception.handler.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "기술 스택", description = "기술 스택 컨트롤러 — 회원의 기술 스택 추가/목록 조회/삭제 엔드포인트를 제공합니다. (수정은 미지원)")
public interface TechStackSwagger {

    @Operation(
            summary = "기술 스택 추가",
            description = "로그인된 회원의 기술 스택을 추가합니다."
    )
    @PostMapping
    ApiResponse<TechStackResponse> addTechStack(
            @Parameter(hidden = true) @LoginMember Member member,
            @Parameter(description = "추가할 기술 스택 정보 (기술명 등)")
            @RequestBody CreateTechStackRequest request);




    @Operation(
            summary = "내 기술 스택 목록 조회",
            description = "로그인된 회원이 등록한 기술 스택 목록을 조회합니다."
    )
    @GetMapping
    ApiResponse<List<TechStackResponse>> getMyTechStacks(
            @Parameter(hidden = true) @LoginMember Member member);




    @Operation(
            summary = "기술 스택 삭제",
            description = "본인이 등록한 기술 스택을 삭제합니다."
    )
    @DeleteMapping("/{techStackId}")
    ApiResponse<String> deleteTechStack(
            @Parameter(hidden = true) @LoginMember Member member,
            @Parameter(description = "삭제할 기술 스택 ID")
            @PathVariable Long techStackId);

}
