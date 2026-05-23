package mju.capstone.ddingconnect.domain.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import mju.capstone.ddingconnect.domain.coffeechat.dto.response.CoffeeChatPartnerResponse;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.global.auth.annotation.LoginMember;
import mju.capstone.ddingconnect.global.response.exception.handler.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Tag(name = "나의 활동", description = "마이페이지 활동 통계 카드 클릭 시 진입하는 '나의 활동' 페이지의 도메인별 본인 스코프 목록 조회 엔드포인트.")
public interface MyActivitySwagger {

    @Operation(
            summary = "나의 커피챗 활동 목록",
            description = "로그인한 회원이 요청자 또는 수신자로 참여한 모든 커피챗을 합쳐 상대방 정보 카드 리스트로 반환합니다. "
                    + "한 카드당 상대방(파트너) 의 닉네임/학과/관심직무/기술스택을 표시합니다. "
                    + "상태(`status`) 는 PENDING/ACCEPTED/REJECTED 모두 포함되며, 화면에서 필터링합니다. "
                    + "마이페이지 '커피챗 수' 카드를 클릭해 진입하는 '나의 활동/커피챗' 화면용입니다."
    )
    @GetMapping("/me/activity/coffeechats")
    ApiResponse<List<CoffeeChatPartnerResponse>> getMyCoffeeChats(
            @Parameter(hidden = true) @LoginMember Member member);
}
