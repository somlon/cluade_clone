package mju.capstone.ddingconnect.domain.coffeechat.service;

import mju.capstone.ddingconnect.domain.coffeechat.dto.request.MatchingRequest;
import mju.capstone.ddingconnect.domain.coffeechat.dto.response.MatchedCandidateDetailResponse;
import mju.capstone.ddingconnect.domain.coffeechat.dto.response.MatchedCandidateResponse;
import mju.capstone.ddingconnect.domain.member.domain.Member;

import java.util.List;

/**
 * [커피챗 매칭 서비스]
 *
 * 커피챗 매칭 플로우의 오케스트레이션 계층. 매칭 알고리즘 호출({@link MatchingAlgorithmClient})과
 * 후보 프로필 조립({@link CandidateProfileAssembler})을 엮어 화면별 응답을 만든다.
 * 매칭 입력값·결과는 저장하지 않으며(무상태 pass-through), 매칭 이력 엔티티는 없다.
 */
public interface CoffeeChatMatchingService {

    /**
     * 매칭 폼(화면 1)으로 알고리즘을 호출해 상위 후보 카드 리스트(화면 2)를 반환한다.
     *
     * @param member  매칭을 요청한 로그인 회원(신청자)
     * @param request 매칭 정보 입력 폼 6필드
     * @return 상위 후보 카드 리스트, 후보가 없으면 빈 리스트
     */
    List<MatchedCandidateResponse> match(Member member, MatchingRequest request);

    /**
     * 매칭 상대 상세(화면 3)를 반환한다. "내 매칭 결과였는지"는 검증하지 않는다.
     *
     * @param memberId 후보 회원 ID
     * @return 후보 상세 응답
     */
    MatchedCandidateDetailResponse getCandidateDetail(Long memberId);
}
