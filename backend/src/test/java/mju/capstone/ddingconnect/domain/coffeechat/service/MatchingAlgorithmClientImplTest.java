package mju.capstone.ddingconnect.domain.coffeechat.service;

import mju.capstone.ddingconnect.domain.coffeechat.dto.request.MatchingRequest;
import mju.capstone.ddingconnect.global.response.exception.handler.CoffeeChatHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("MatchingAlgorithmClientImpl 단위 테스트")
class MatchingAlgorithmClientImplTest {

    private static final String ENDPOINT_URI = "/api/data/coffeechat/match";
    private static final Long REQUESTER_ID = 1L;
    private static final Long CANDIDATE_ID_1 = 101L;
    private static final Long CANDIDATE_ID_2 = 102L;
    private static final Long CANDIDATE_ID_3 = 103L;

    private MockRestServiceServer server;
    private MatchingAlgorithmClientImpl client;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
        client = new MatchingAlgorithmClientImpl(restClientBuilder.build());
    }

    @Test
    @DisplayName("응답 top_matches 의 id 만 추출해 받은 순서대로 반환한다(데이터 파트가 이미 정렬)")
    void extractsIdsInOrderFromTopMatches() {
        server.expect(requestTo(ENDPOINT_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(successResponse(List.of(
                        topMatchJson(CANDIDATE_ID_1, 90.0),
                        topMatchJson(CANDIDATE_ID_2, 50.0),
                        topMatchJson(CANDIDATE_ID_3, 10.0))),
                        MediaType.APPLICATION_JSON));

        List<Long> result = client.topMatches(sampleForm(), REQUESTER_ID);

        assertThat(result).containsExactly(CANDIDATE_ID_1, CANDIDATE_ID_2, CANDIDATE_ID_3);
        server.verify();
    }

    @Test
    @DisplayName("데이터 파트가 후보 0건 반환 시 빈 리스트를 그대로 반환한다(예외 아님)")
    void returnsEmptyListWhenNoMatches() {
        server.expect(requestTo(ENDPOINT_URI))
                .andRespond(withSuccess(successResponse(List.of()), MediaType.APPLICATION_JSON));

        List<Long> result = client.topMatches(sampleForm(), REQUESTER_ID);

        assertThat(result).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("요청 본문은 데이터 파트 RecommendInput 스키마(6필드 플랫, snake_case tech_stacks) 와 정합")
    void requestBodyMatchesRecommendInputSchema() {
        server.expect(requestTo(ENDPOINT_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.year").value("3"))                    // Integer 3 → String "3"
                .andExpect(jsonPath("$.gpa").value("4.0"))
                .andExpect(jsonPath("$.major").value("응용소프트웨어학과"))
                .andExpect(jsonPath("$.job").value("백엔드 개발자"))
                .andExpect(jsonPath("$.tech_stacks").isArray())
                .andExpect(jsonPath("$.goal").value("카카오"))
                .andRespond(withSuccess(successResponse(List.of()), MediaType.APPLICATION_JSON));

        client.topMatches(sampleForm(), REQUESTER_ID);

        server.verify();
    }

    @Test
    @DisplayName("폼 capability 콤마 토큰은 trim·대문자·TechStackName 화이트리스트로 정규화한다")
    void normalizesCapabilityTokens() {
        // 소문자·공백·중복·미지원(한글) 토큰 혼합
        MatchingRequest form = new MatchingRequest(3, "4.0", "응용소프트웨어학과",
                "백엔드 개발자",
                "  java , spring,  알수없는스택, JAVA, react ",
                "카카오");

        server.expect(requestTo(ENDPOINT_URI))
                .andExpect(jsonPath("$.tech_stacks", org.hamcrest.Matchers.containsInAnyOrder(
                        "JAVA", "SPRING", "REACT")))  // 중복 JAVA 제거, 미지원 한글 버림
                .andRespond(withSuccess(successResponse(List.of()), MediaType.APPLICATION_JSON));

        client.topMatches(form, REQUESTER_ID);

        server.verify();
    }

    @Test
    @DisplayName("폼 grade 가 null 이면 year 를 빈 문자열로 직렬화(Pydantic str 필수)")
    void serializesNullGradeAsEmptyString() {
        MatchingRequest form = new MatchingRequest(null, "4.0", "응용소프트웨어학과",
                "백엔드 개발자", "JAVA", "카카오");

        server.expect(requestTo(ENDPOINT_URI))
                .andExpect(jsonPath("$.year").value(""))
                .andRespond(withSuccess(successResponse(List.of()), MediaType.APPLICATION_JSON));

        client.topMatches(form, REQUESTER_ID);

        server.verify();
    }

    @Test
    @DisplayName("알고리즘 호출 실패(500) 시 MATCHING_ALGORITHM_FAILED 로 변환한다")
    void rethrowsAlgorithmFailureAsCoffeeChatHandler() {
        server.expect(requestTo(ENDPOINT_URI)).andRespond(withServerError());

        assertThatThrownBy(() -> client.topMatches(sampleForm(), REQUESTER_ID))
                .isInstanceOf(CoffeeChatHandler.class);

        server.verify();
    }

    @Test
    @DisplayName("응답 top_matches 가 null 인 경우 MATCHING_ALGORITHM_FAILED 로 변환한다")
    void rethrowsNullTopMatchesAsCoffeeChatHandler() {
        server.expect(requestTo(ENDPOINT_URI))
                .andRespond(withSuccess("{\"status\":\"success\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.topMatches(sampleForm(), REQUESTER_ID))
                .isInstanceOf(CoffeeChatHandler.class);

        server.verify();
    }

    // ===== Fixture helpers =====

    private MatchingRequest sampleForm() {
        return new MatchingRequest(3, "4.0", "응용소프트웨어학과",
                "백엔드 개발자", "JAVA, SPRING", "카카오");
    }

    /** 데이터 파트 RecommendOutput JSON 직렬화 (top_matches 배열). */
    private String successResponse(List<String> topMatchJsonList) {
        return "{\"status\":\"success\",\"top_matches\":["
                + String.join(",", topMatchJsonList) + "]}";
    }

    /** 데이터 파트 TopMatchItem 한 장 — id 와 match_score 외 부가 필드는 백엔드가 무시. */
    private String topMatchJson(Long id, double matchScore) {
        return "{"
                + "\"id\":" + id + ","
                + "\"name\":\"이선배\","
                + "\"department\":\"학과 미상\","
                + "\"company\":\"카카오\","
                + "\"job\":\"BACKEND\","
                + "\"career\":\"3년차\","
                + "\"location\":\"위치 미상\","
                + "\"tech_stacks\":[\"JAVA\",\"SPRING\"],"
                + "\"match_score\":" + matchScore
                + "}";
    }
}
