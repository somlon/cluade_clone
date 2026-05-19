package mju.capstone.ddingconnect.domain.coffeechat.service;

import mju.capstone.ddingconnect.domain.coffeechat.dto.request.MatchingRequest;

import java.util.List;

/**
 * [매칭 알고리즘 클라이언트]
 * 커피챗 매칭 폼을 외부 유사도 알고리즘(타 파트)으로 전달하고
 * 상위 후보 회원 ID 리스트를 받아오는 아웃바운드 클라이언트.
 *
 * 알고리즘은 자체 DB 조회로 후보를 찾아 회원 ID 리스트만 반환한다(점수 없음).
 * 엔드포인트 URL·요청/응답 JSON 스키마는 타 파트 협의 미확정 상태이며,
 * 구현체가 이 인터페이스 뒤로 추상화한다.
 */
public interface MatchingAlgorithmClient {

    /**
     * 매칭 알고리즘을 호출해 상위 후보 회원 ID 리스트를 반환한다.
     *
     * @param form        매칭 정보 입력 폼(6필드)
     * @param requesterId 신청자 회원 ID
     * @return 후보 회원 ID 리스트
     */
    List<Long> topMatches(MatchingRequest form, Long requesterId);
}
