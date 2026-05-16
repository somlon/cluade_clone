package mju.capstone.ddingconnect.domain.techstack.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.techstack.dto.request.ReplaceTechStackRequest;
import mju.capstone.ddingconnect.domain.techstack.dto.response.TechStackResponse;
import mju.capstone.ddingconnect.global.auth.annotation.LoginMember;
import mju.capstone.ddingconnect.global.response.exception.handler.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "기술 스택", description = "기술 스택 컨트롤러 — 회원의 기술 스택 일괄 교체(REPLACE)/목록 조회 엔드포인트를 제공합니다.")
public interface TechStackSwagger {

    @Operation(
            summary = "기술 스택 일괄 교체",
            description = "로그인된 회원의 기술 스택 전체를 요청 리스트로 교체(REPLACE)합니다. "
                    + "빈 리스트는 전부 삭제를 의미하며, 리스트 내부 중복은 서버가 제거합니다. names 가 null 이면 400 입니다."
    )
    @PatchMapping
    ApiResponse<List<TechStackResponse>> replaceTechStacks(
            @Parameter(hidden = true) @LoginMember Member member,
            @Parameter(description = "교체할 기술 스택 전체 리스트")
            @RequestBody ReplaceTechStackRequest request);


    @Operation(
            summary = "내 기술 스택 목록 조회",
            description = "로그인된 회원이 등록한 기술 스택 목록을 조회합니다."
    )
    @GetMapping
    ApiResponse<List<TechStackResponse>> getMyTechStacks(
            @Parameter(hidden = true) @LoginMember Member member);
}
