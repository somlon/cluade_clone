package mju.capstone.ddingconnect.domain.member.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * [포인트 충전 상품]
 * 포인트 충전 페이지(`GET /api/v1/members/me/point/products`)에 노출되는 충전 상품 목록.
 * 단위는 P로 통일하며, 프론트의 "토큰" 표기는 동일 값을 다른 라벨로만 렌더링한다.
 * 운영 중 가격 변동/추천 뱃지 위치 변경이 발생하면 이 enum 만 수정하면 된다(A안 — 상수 enum).
 */
@Getter
@RequiredArgsConstructor
public enum PointProduct {

    P_10(1L, 10, 1_000, false),
    P_30(2L, 30, 3_000, false),
    P_50(3L, 50, 5_000, true),   // "추천" 뱃지
    P_100(4L, 100, 10_000, false),
    P_300(5L, 300, 30_000, false),
    P_500(6L, 500, 50_000, false);

    private final Long id;
    private final Integer points;   // 충전될 P
    private final Integer price;    // 결제 금액 (원)
    private final boolean recommended;
}
