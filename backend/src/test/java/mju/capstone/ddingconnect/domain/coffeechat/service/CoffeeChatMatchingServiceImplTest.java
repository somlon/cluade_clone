package mju.capstone.ddingconnect.domain.coffeechat.service;

import mju.capstone.ddingconnect.domain.coffeechat.domain.CoffeeChat;
import mju.capstone.ddingconnect.domain.coffeechat.domain.CoffeeChatStatus;
import mju.capstone.ddingconnect.domain.coffeechat.domain.repository.CoffeeChatRepository;
import mju.capstone.ddingconnect.domain.coffeechat.dto.request.MatchingRequest;
import mju.capstone.ddingconnect.domain.coffeechat.dto.response.MatchedCandidateDetailResponse;
import mju.capstone.ddingconnect.domain.coffeechat.dto.response.MatchedCandidateResponse;
import mju.capstone.ddingconnect.domain.coffeechat.dto.response.MyActivityCoffeeChatResponse;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static mju.capstone.ddingconnect.domain.coffeechat.CoffeeChatMatchingTestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CoffeeChatMatchingServiceImpl 단위 테스트")
class CoffeeChatMatchingServiceImplTest {

    private static final Long REQUESTER_ID = 1L;

    @Mock MatchingAlgorithmClient matchingAlgorithmClient;
    @Mock CandidateProfileAssembler candidateProfileAssembler;
    @Mock CoffeeChatRepository coffeeChatRepository;
    @InjectMocks CoffeeChatMatchingServiceImpl matchingService;

    private Member requester;

    @BeforeEach
    void setUp() {
        requester = Member.builder().id(REQUESTER_ID).build();
    }

    @Test
    @DisplayName("match - 알고리즘이 반환한 후보 ID 순서대로 카드 DTO 를 조립해 반환한다")
    void matchAssemblesCandidateCards() {
        MatchingRequest form = sampleForm();
        when(matchingAlgorithmClient.topMatches(form, REQUESTER_ID))
                .thenReturn(List.of(CANDIDATE_ID_1, CANDIDATE_ID_2, CANDIDATE_ID_3));
        when(candidateProfileAssembler.assembleCard(CANDIDATE_ID_1)).thenReturn(candidateCard(CANDIDATE_ID_1));
        when(candidateProfileAssembler.assembleCard(CANDIDATE_ID_2)).thenReturn(candidateCard(CANDIDATE_ID_2));
        when(candidateProfileAssembler.assembleCard(CANDIDATE_ID_3)).thenReturn(candidateCard(CANDIDATE_ID_3));

        List<MatchedCandidateResponse> result = matchingService.match(requester, form);

        assertThat(result).extracting(MatchedCandidateResponse::memberId)
                .containsExactly(CANDIDATE_ID_1, CANDIDATE_ID_2, CANDIDATE_ID_3);
        verify(matchingAlgorithmClient).topMatches(form, REQUESTER_ID);
    }

    @Test
    @DisplayName("match - 알고리즘 후보가 없으면 빈 리스트를 반환하고 조립기를 호출하지 않는다")
    void matchReturnsEmptyWhenNoCandidates() {
        MatchingRequest form = sampleForm();
        when(matchingAlgorithmClient.topMatches(form, REQUESTER_ID)).thenReturn(List.of());

        List<MatchedCandidateResponse> result = matchingService.match(requester, form);

        assertThat(result).isEmpty();
        verify(candidateProfileAssembler, never()).assembleCard(any());
    }

    @Test
    @DisplayName("getCandidateDetail - memberId 로 상세 DTO 조립을 조립기에 위임한다")
    void getCandidateDetailDelegatesToAssembler() {
        when(candidateProfileAssembler.assembleDetail(CANDIDATE_ID_1))
                .thenReturn(candidateDetail(CANDIDATE_ID_1));

        MatchedCandidateDetailResponse result = matchingService.getCandidateDetail(CANDIDATE_ID_1);

        assertThat(result.memberId()).isEqualTo(CANDIDATE_ID_1);
        verify(candidateProfileAssembler).assembleDetail(CANDIDATE_ID_1);
    }

    @Test
    @DisplayName("getMyActivity - 신청자=본인 & status=ACCEPTED 커피챗의 수신자를 경량 카드 DTO 로 조립한다 (jobType 포함, 상세 조립기 미사용)")
    void getMyActivityReturnsAcceptedReceiverCards() {
        Member candidate1 = Member.builder().id(CANDIDATE_ID_1).build();
        Member candidate2 = Member.builder().id(CANDIDATE_ID_2).build();
        CoffeeChat chat1 = CoffeeChat.builder().requester(requester).receiver(candidate1)
                .status(CoffeeChatStatus.ACCEPTED).build();
        CoffeeChat chat2 = CoffeeChat.builder().requester(requester).receiver(candidate2)
                .status(CoffeeChatStatus.ACCEPTED).build();
        when(coffeeChatRepository.findByRequesterIdAndStatus(REQUESTER_ID, CoffeeChatStatus.ACCEPTED))
                .thenReturn(List.of(chat1, chat2));
        when(candidateProfileAssembler.assembleMyActivityCard(CANDIDATE_ID_1))
                .thenReturn(myActivityCard(CANDIDATE_ID_1));
        when(candidateProfileAssembler.assembleMyActivityCard(CANDIDATE_ID_2))
                .thenReturn(myActivityCard(CANDIDATE_ID_2));

        List<MyActivityCoffeeChatResponse> result = matchingService.getMyActivity(requester);

        assertThat(result).extracting(MyActivityCoffeeChatResponse::memberId)
                .containsExactly(CANDIDATE_ID_1, CANDIDATE_ID_2);
        assertThat(result).extracting(MyActivityCoffeeChatResponse::jobType)
                .containsOnly(CANDIDATE_JOB_TYPE);
        verify(candidateProfileAssembler, never()).assembleDetail(any());
    }

    @Test
    @DisplayName("getMyActivity - 수락된 커피챗이 없으면 빈 리스트를 반환한다")
    void getMyActivityReturnsEmptyWhenNone() {
        when(coffeeChatRepository.findByRequesterIdAndStatus(REQUESTER_ID, CoffeeChatStatus.ACCEPTED))
                .thenReturn(List.of());

        List<MyActivityCoffeeChatResponse> result = matchingService.getMyActivity(requester);

        assertThat(result).isEmpty();
    }
}
