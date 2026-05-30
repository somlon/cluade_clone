package mju.capstone.ddingconnect.domain.coffeechat.service;

import lombok.RequiredArgsConstructor;
import mju.capstone.ddingconnect.domain.coffeechat.domain.CoffeeChatStatus;
import mju.capstone.ddingconnect.domain.coffeechat.domain.repository.CoffeeChatRepository;
import mju.capstone.ddingconnect.domain.coffeechat.dto.request.MatchingRequest;
import mju.capstone.ddingconnect.domain.coffeechat.dto.response.MatchedCandidateDetailResponse;
import mju.capstone.ddingconnect.domain.coffeechat.dto.response.MatchedCandidateResponse;
import mju.capstone.ddingconnect.domain.coffeechat.dto.response.MyActivityCoffeeChatResponse;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * [커피챗 매칭 서비스 구현체]
 *
 * 매칭 플로우 오케스트레이션:
 * <ul>
 *   <li>{@code match} — 매칭 폼을 {@link MatchingAlgorithmClient} 로 외부 알고리즘에 전달해
 *       후보 회원 ID 리스트를 받고, 각 ID 를 {@link CandidateProfileAssembler} 로 카드 DTO 조립</li>
 *   <li>{@code getCandidateDetail} — memberId 로 후보 상세 DTO 조립</li>
 *   <li>{@code getMyActivity} — 신청자=본인 & status=ACCEPTED 커피챗의 수신자를 경량 카드 DTO 조립</li>
 * </ul>
 *
 * <p>{@code match} 는 외부 HTTP 호출을 포함하므로 트랜잭션으로 감싸지 않는다 —
 * DB 조회 트랜잭션은 {@link CandidateProfileAssembler} 의 read-only 메서드가 각각 담당한다.
 */
@Service
@RequiredArgsConstructor
public class CoffeeChatMatchingServiceImpl implements CoffeeChatMatchingService {

    private final MatchingAlgorithmClient matchingAlgorithmClient;
    private final CandidateProfileAssembler candidateProfileAssembler;
    private final CoffeeChatRepository coffeeChatRepository;

    @Override
    public List<MatchedCandidateResponse> match(Member member, MatchingRequest request) {
        return matchingAlgorithmClient.topMatches(request, member.getId()).stream()
                .map(candidateProfileAssembler::assembleCard)
                .toList();
    }

    @Override
    public MatchedCandidateDetailResponse getCandidateDetail(Long memberId) {
        return candidateProfileAssembler.assembleDetail(memberId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MyActivityCoffeeChatResponse> getMyActivity(Member member) {
        return coffeeChatRepository.findByRequesterIdAndStatus(member.getId(), CoffeeChatStatus.ACCEPTED)
                .stream()
                .map(coffeeChat -> candidateProfileAssembler.assembleMyActivityCard(coffeeChat.getReceiver().getId()))
                .toList();
    }
}
