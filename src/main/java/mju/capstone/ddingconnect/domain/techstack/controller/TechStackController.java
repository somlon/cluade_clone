package mju.capstone.ddingconnect.domain.techstack.controller;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.techstack.dto.request.CreateTechStackRequest;
import mju.capstone.ddingconnect.domain.techstack.dto.response.TechStackResponse;
import mju.capstone.ddingconnect.domain.techstack.service.TechStackService;
import mju.capstone.ddingconnect.global.auth.annotation.LoginMember;
import mju.capstone.ddingconnect.global.response.exception.handler.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * [기술 스택 컨트롤러]
 *
 * <p>수정(UPDATE) 엔드포인트를 제공하지 않는 이유:
 * TechStack 엔티티의 실질적인 데이터 컬럼은 {@code name}(TechStackName ENUM) 하나뿐입니다.
 * ENUM 값 하나를 다른 ENUM 값으로 바꾸는 행위는 의미상 "수정"이 아니라
 * "기존 스택 삭제(DELETE) + 새 스택 추가(POST)"와 동일합니다.
 * 따라서 별도의 PATCH 엔드포인트 없이 삭제 후 재등록 패턴으로 충분히 커버됩니다.
 */
@RestController
@RequestMapping("/api/v1/tech-stacks")
@RequiredArgsConstructor
public class TechStackController implements TechStackSwagger {

    private final TechStackService techStackService;

    /** 기술 스택 추가 (Create) */
    @PostMapping
    public ApiResponse<TechStackResponse> addTechStack(
            @Parameter(hidden = true) @LoginMember Member member,
            @RequestBody CreateTechStackRequest request) {
        return ApiResponse.onSuccess(techStackService.add(member, request));
    }

    /** 내 기술 스택 목록 조회 (Read) */
    @GetMapping
    public ApiResponse<List<TechStackResponse>> getMyTechStacks(
            @Parameter(hidden = true) @LoginMember Member member) {
        return ApiResponse.onSuccess(techStackService.getMyTechStacks(member));
    }

    /** 기술 스택 삭제 (Delete) */
    @DeleteMapping("/{techStackId}")
    public ApiResponse<String> deleteTechStack(
            @Parameter(hidden = true) @LoginMember Member member,
            @PathVariable Long techStackId) {
        techStackService.delete(member, techStackId);
        return ApiResponse.onSuccess("기술 스택이 삭제되었습니다.");
    }
}
