package mju.capstone.ddingconnect.domain.coffeechat.service;

import mju.capstone.ddingconnect.domain.coffeechat.dto.request.MatchingRequest;
import mju.capstone.ddingconnect.domain.job_post.domain.JobType;
import mju.capstone.ddingconnect.domain.member.domain.Graduate;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.member.domain.MemberRole;
import mju.capstone.ddingconnect.domain.member.domain.repository.GraduateRepository;
import mju.capstone.ddingconnect.domain.member.domain.repository.MemberRepository;
import mju.capstone.ddingconnect.domain.techstack.domain.TechStack;
import mju.capstone.ddingconnect.domain.techstack.domain.TechStackName;
import mju.capstone.ddingconnect.domain.techstack.domain.repository.TechStackRepository;
import mju.capstone.ddingconnect.global.response.exception.handler.CoffeeChatHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
@DisplayName("MatchingAlgorithmClientImpl 단위 테스트")
class MatchingAlgorithmClientImplTest {

    private static final String ENDPOINT_URI = "/api/data/coffeechat/match";
    private static final int TOP_N = 2;
    private static final Long REQUESTER_ID = 1L;
    private static final Long GRADUATE_ID_HIGH = 101L;
    private static final Long GRADUATE_ID_MID = 102L;
    private static final Long GRADUATE_ID_LOW = 103L;
    private static final Long GRADUATE_NO_JOB_TYPE = 201L;
    private static final Long GRADUATE_NO_COMPANY = 202L;
    private static final Long GRADUATE_NO_TECHSTACK = 203L;

    private static final String SUCCESS_RESPONSE_TEMPLATE = """
            {
              "status": "success",
              "match_results": {
                "jobScore": 100.0,
                "ability": 100.0,
                "goal": 100.0,
                "totalMatchRate": %s
              }
            }
            """;

    @Mock MemberRepository memberRepository;
    @Mock GraduateRepository graduateRepository;
    @Mock TechStackRepository techStackRepository;

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer server;
    private MatchingAlgorithmClientImpl client;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        // 후보 N명 호출 순서가 입력 리스트 순서에 의존하지 않도록 호출 순서를 검증하지 않는다.
        server = MockRestServiceServer.bindTo(restClientBuilder).ignoreExpectOrder(true).build();
        RestClient restClient = restClientBuilder.build();
        client = new MatchingAlgorithmClientImpl(
                restClient, TOP_N, memberRepository, graduateRepository, techStackRepository);
    }

    @Test
    @DisplayName("후보 풀이 비어 있으면 알고리즘을 호출하지 않고 빈 리스트를 반환한다")
    void emptyCandidatePoolReturnsEmpty() {
        when(memberRepository.findAllByRole(MemberRole.GRADUATE)).thenReturn(List.of());
        server.expect(org.springframework.test.web.client.ExpectedCount.never(), requestTo(ENDPOINT_URI));

        List<Long> result = client.topMatches(sampleForm(), REQUESTER_ID);

        assertThat(result).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("후보를 totalMatchRate DESC 정렬해 상위 top-n 만 반환한다")
    void sortsByTotalMatchRateAndAppliesTopN() {
        givenGraduate(GRADUATE_ID_HIGH, "삼성");
        givenGraduate(GRADUATE_ID_MID, "네이버");
        givenGraduate(GRADUATE_ID_LOW, "카카오");
        when(memberRepository.findAllByRole(MemberRole.GRADUATE))
                .thenReturn(List.of(member(GRADUATE_ID_LOW), member(GRADUATE_ID_HIGH), member(GRADUATE_ID_MID)));

        // 회사명 매칭(고유 식별자) 기준으로 응답 매핑 — 점수 분포: HIGH 90 / MID 50 / LOW 10
        expectMatchCallReturning("\"삼성\"", 90.0);
        expectMatchCallReturning("\"네이버\"", 50.0);
        expectMatchCallReturning("\"카카오\"", 10.0);

        List<Long> result = client.topMatches(sampleForm(), REQUESTER_ID);

        assertThat(result).containsExactly(GRADUATE_ID_HIGH, GRADUATE_ID_MID);
        server.verify();
    }

    @Test
    @DisplayName("매칭 데이터 누락 후보(jobType null·company blank·TechStack 0건)는 풀에서 제외한다")
    void filtersOutCandidatesWithEmptyMatchingData() {
        givenGraduate(GRADUATE_ID_HIGH, "삼성"); // 정상 후보
        // jobType null — jobType 체크에서 즉시 탈락, techStack 조회 도달 X
        when(graduateRepository.findByMemberId(GRADUATE_NO_JOB_TYPE))
                .thenReturn(Optional.of(Graduate.builder()
                        .id(GRADUATE_NO_JOB_TYPE).jobType(null).company("어딘가").build()));
        // company blank — company 체크에서 탈락, techStack 조회 도달 X
        when(graduateRepository.findByMemberId(GRADUATE_NO_COMPANY))
                .thenReturn(Optional.of(Graduate.builder()
                        .id(GRADUATE_NO_COMPANY).jobType(JobType.BACKEND).company("  ").build()));
        // tech stack 0건 — 마지막 체크에서 탈락
        when(graduateRepository.findByMemberId(GRADUATE_NO_TECHSTACK))
                .thenReturn(Optional.of(Graduate.builder()
                        .id(GRADUATE_NO_TECHSTACK).jobType(JobType.BACKEND).company("어딘가").build()));
        when(techStackRepository.findByMemberId(GRADUATE_NO_TECHSTACK))
                .thenReturn(List.of());

        when(memberRepository.findAllByRole(MemberRole.GRADUATE)).thenReturn(List.of(
                member(GRADUATE_NO_JOB_TYPE), member(GRADUATE_NO_COMPANY),
                member(GRADUATE_NO_TECHSTACK), member(GRADUATE_ID_HIGH)));

        expectMatchCallReturning("\"삼성\"", 70.0);

        List<Long> result = client.topMatches(sampleForm(), REQUESTER_ID);

        assertThat(result).containsExactly(GRADUATE_ID_HIGH);
        server.verify();
    }

    @Test
    @DisplayName("신청자 본인은 후보 풀에서 제외한다(GRADUATE 역할이라도)")
    void excludesRequesterFromCandidatePool() {
        // requester 본인은 ID 일치로 graduate/tech 조회 전에 필터링되므로 stub 불필요
        givenGraduate(GRADUATE_ID_HIGH, "삼성");
        when(memberRepository.findAllByRole(MemberRole.GRADUATE))
                .thenReturn(List.of(member(REQUESTER_ID), member(GRADUATE_ID_HIGH)));

        expectMatchCallReturning("\"삼성\"", 80.0);

        List<Long> result = client.topMatches(sampleForm(), REQUESTER_ID);

        assertThat(result).containsExactly(GRADUATE_ID_HIGH);
        server.verify();
    }

    @Test
    @DisplayName("요청 본문은 양방향 페어 {requester, receiver} 스키마 — snake_case user_id·tech_stacks")
    void requestBodyUsesBidirectionalPairSchema() {
        givenGraduate(GRADUATE_ID_HIGH, "삼성");
        when(memberRepository.findAllByRole(MemberRole.GRADUATE))
                .thenReturn(List.of(member(GRADUATE_ID_HIGH)));

        server.expect(requestTo(ENDPOINT_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.requester.user_id").value(REQUESTER_ID))
                .andExpect(jsonPath("$.requester.job").value("백엔드 개발자"))
                .andExpect(jsonPath("$.requester.tech_stacks").isArray())
                .andExpect(jsonPath("$.requester.goal").value("카카오"))
                .andExpect(jsonPath("$.receiver.user_id").value(GRADUATE_ID_HIGH))
                .andExpect(jsonPath("$.receiver.job").value("BACKEND"))
                .andExpect(jsonPath("$.receiver.tech_stacks[0]").value("JAVA"))
                .andExpect(jsonPath("$.receiver.goal").value("삼성"))
                .andRespond(withSuccess(String.format(SUCCESS_RESPONSE_TEMPLATE, "50.0"),
                        MediaType.APPLICATION_JSON));

        client.topMatches(sampleForm(), REQUESTER_ID);

        server.verify();
    }

    @Test
    @DisplayName("폼 capability 콤마 토큰은 trim·대문자·TechStackName 화이트리스트로 정규화한다")
    void normalizesCapabilityTokens() {
        givenGraduate(GRADUATE_ID_HIGH, "삼성");
        when(memberRepository.findAllByRole(MemberRole.GRADUATE))
                .thenReturn(List.of(member(GRADUATE_ID_HIGH)));

        MatchingRequest form = new MatchingRequest(3, "4.0", "응용소프트웨어학과",
                "백엔드 개발자",
                "  java , spring,  알수없는스택, JAVA, react ",  // 소문자·공백·중복·미지원 토큰 혼합
                "카카오");

        server.expect(requestTo(ENDPOINT_URI))
                // JAVA·SPRING·REACT 만 살아남고 (중복 JAVA 제거, 미지원 한글 토큰 버림), 대문자 정규화
                .andExpect(jsonPath("$.requester.tech_stacks", org.hamcrest.Matchers.containsInAnyOrder(
                        "JAVA", "SPRING", "REACT")))
                .andRespond(withSuccess(String.format(SUCCESS_RESPONSE_TEMPLATE, "10.0"),
                        MediaType.APPLICATION_JSON));

        client.topMatches(form, REQUESTER_ID);

        server.verify();
    }

    @Test
    @DisplayName("알고리즘 호출 실패 시 MATCHING_ALGORITHM_FAILED 로 변환한다")
    void rethrowsAlgorithmFailureAsCoffeeChatHandler() {
        givenGraduate(GRADUATE_ID_HIGH, "삼성");
        when(memberRepository.findAllByRole(MemberRole.GRADUATE))
                .thenReturn(List.of(member(GRADUATE_ID_HIGH)));

        server.expect(requestTo(ENDPOINT_URI)).andRespond(withServerError());

        assertThatThrownBy(() -> client.topMatches(sampleForm(), REQUESTER_ID))
                .isInstanceOf(CoffeeChatHandler.class);

        server.verify();
    }

    // ===== Fixture helpers =====

    private MatchingRequest sampleForm() {
        return new MatchingRequest(3, "4.0", "응용소프트웨어학과",
                "백엔드 개발자", "JAVA, SPRING", "카카오");
    }

    private Member member(Long id) {
        return Member.builder().id(id).role(MemberRole.GRADUATE).build();
    }

    private TechStack techStack(TechStackName name) {
        return TechStack.builder().name(name).build();
    }

    /** 정상 후보 한 명을 mock(graduateRepository, techStackRepository) 에 등록한다. */
    private void givenGraduate(Long memberId, String company) {
        when(graduateRepository.findByMemberId(memberId))
                .thenReturn(Optional.of(Graduate.builder()
                        .id(memberId).jobType(JobType.BACKEND).company(company).build()));
        when(techStackRepository.findByMemberId(memberId))
                .thenReturn(List.of(techStack(TechStackName.JAVA)));
    }

    /** receiver.goal(회사명 JSON 리터럴, 따옴표 포함)이 일치할 때 지정 totalMatchRate 로 응답하도록 expect 등록. */
    private void expectMatchCallReturning(String goalJsonLiteral, double totalMatchRate) {
        server.expect(requestTo(ENDPOINT_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "\"goal\":" + goalJsonLiteral)))
                .andRespond(withSuccess(
                        String.format(SUCCESS_RESPONSE_TEMPLATE, String.valueOf(totalMatchRate)),
                        MediaType.APPLICATION_JSON));
    }
}
