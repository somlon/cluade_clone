package mju.capstone.ddingconnect.domain.member.dto.response;

/**
 * [포인트 잔액 응답 DTO]
 * 홈 화면 포인트 모달(두 번째 사진) 진입 시 최신 보유 P를 조회한다.
 * 단위는 P로 통일.
 */
public record PointBalanceResponse(
        Long point
) {}
