package mju.capstone.ddingconnect.domain.coffeechat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import mju.capstone.ddingconnect.domain.coffeechat.dto.request.MatchingRequest;
import mju.capstone.ddingconnect.domain.coffeechat.dto.response.MatchedCandidateDetailResponse;
import mju.capstone.ddingconnect.domain.coffeechat.dto.response.MatchedCandidateResponse;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.global.auth.annotation.LoginMember;
import mju.capstone.ddingconnect.global.response.exception.handler.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "커피챗 매칭", description = "커피챗 매칭 컨트롤러 — 매칭 정보 입력으로 후보 추천/매칭 상대 상세 조회/나의 활동(수락된 커피챗) 조회 엔드포인트를 제공합니다.")
public interface CoffeeChatMatchingSwagger {

    @Operation(
            summary = "커피챗 매칭 요청",
            description = "매칭 정보 입력 폼(학년·학점·전공·관심 직무·보유 역량·목표 기업)을 받아 매칭 알고리즘을 호출하고, 상위 후보 카드 리스트를 반환합니다. 로그인된 회원만 호출할 수 있습니다. 후보가 없으면 빈 리스트를 반환합니다."
    )
    @PostMapping("/matching")
    ApiResponse<List<MatchedCandidateResponse>> match(
            @Parameter(hidden = true) @LoginMember Member member,
            @Parameter(description = "매칭 정보 입력 폼 6필드")
            @RequestBody MatchingRequest request);




    @Operation(
            summary = "매칭 상대 상세 조회",
            description = "매칭 후보 회원의 상세 프로필(포트폴리오·소셜 링크·명함·공고 포함)을 조회합니다."
    )
    @GetMapping("/matching/{memberId}")
    ApiResponse<MatchedCandidateDetailResponse> getCandidateDetail(
            @Parameter(description = "상세를 조회할 후보 회원 ID")
            @PathVariable Long memberId);




    @Operation(
            summary = "나의 활동 › 커피챗 조회",
            description = "로그인된 회원이 신청자이고 수락(ACCEPTED)된 커피챗 상대 목록을 상세 프로필로 조회합니다. 없으면 빈 리스트를 반환합니다."
    )
    @GetMapping("/my-activity")
    ApiResponse<List<MatchedCandidateDetailResponse>> getMyActivity(
            @Parameter(hidden = true) @LoginMember Member member);

}
