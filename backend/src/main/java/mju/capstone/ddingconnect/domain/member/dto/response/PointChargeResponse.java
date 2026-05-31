package mju.capstone.ddingconnect.domain.member.dto.response;

import mju.capstone.ddingconnect.domain.member.domain.PointProduct;

import java.util.Arrays;
import java.util.List;

/**
 * [포인트 충전 페이지 응답 DTO]
 * 충전 페이지(세 번째 사진) 진입 시 보유 P + 충전 상품 목록을 1회 호출로 제공한다.
 * - point   : 상단 "내 포인트" 영역 표시값
 * - products: 충전 상품 카드 리스트 (PointProduct enum 기반 — A안)
 */
public record PointChargeResponse(
        Long point,
        List<ProductItem> products
) {

    /**
     * 충전 상품 카드 1건.
     *
     * @param id          상품 식별자 (결제 요청 시 사용 예정)
     * @param points      충전될 P (단위 P로 통일 — 프론트 "토큰" 라벨은 동일 값 다른 표기)
     * @param price       결제 금액 (원)
     * @param recommended "추천" 뱃지 노출 여부
     */
    public record ProductItem(
            Long id,
            Integer points,
            Integer price,
            Boolean recommended
    ) {}

    /** 현재 보유 P + 전체 충전 상품 enum 을 매핑해 응답을 만든다. */
    public static PointChargeResponse of(Long currentPoint) {
        List<ProductItem> products = Arrays.stream(PointProduct.values())
                .map(p -> new ProductItem(p.getId(), p.getPoints(), p.getPrice(), p.isRecommended()))
                .toList();
        return new PointChargeResponse(currentPoint, products);
    }
}
