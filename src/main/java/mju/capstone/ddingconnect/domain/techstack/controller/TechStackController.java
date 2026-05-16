package mju.capstone.ddingconnect.domain.techstack.controller;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.techstack.dto.request.ReplaceTechStackRequest;
import mju.capstone.ddingconnect.domain.techstack.dto.response.TechStackResponse;
import mju.capstone.ddingconnect.domain.techstack.service.TechStackService;
import mju.capstone.ddingconnect.global.auth.annotation.LoginMember;
import mju.capstone.ddingconnect.global.response.exception.handler.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * [기술 스택 컨트롤러]
 * 마이페이지 수정 화면이 유일한 진입점이라, 단건 add/delete 대신
 * "수정 완료" 시점의 일괄 교체(REPLACE) 와 목록 조회만 제공한다.
 */
@RestController
@RequestMapping("/api/v1/tech-stacks")
@RequiredArgsConstructor
public class TechStackController implements TechStackSwagger {

    private final TechStackService techStackService;

    /** 기술 스택 일괄 교체 (REPLACE) */
    @PatchMapping
    public ApiResponse<List<TechStackResponse>> replaceTechStacks(
            @Parameter(hidden = true) @LoginMember Member member,
            @RequestBody ReplaceTechStackRequest request) {
        return ApiResponse.onSuccess(techStackService.replace(member, request));
    }

    /** 내 기술 스택 목록 조회 (Read) */
    @GetMapping
    public ApiResponse<List<TechStackResponse>> getMyTechStacks(
            @Parameter(hidden = true) @LoginMember Member member) {
        return ApiResponse.onSuccess(techStackService.getMyTechStacks(member));
    }
}
