package mju.capstone.ddingconnect.domain.coffeechat.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import mju.capstone.ddingconnect.domain.coffeechat.dto.request.MatchingRequest;
import mju.capstone.ddingconnect.domain.techstack.domain.TechStackName;
import mju.capstone.ddingconnect.global.response.code.status.ErrorStatus;
import mju.capstone.ddingconnect.global.response.exception.handler.CoffeeChatHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * [매칭 알고리즘 클라이언트 구현체 — RestClient]
 *
 * 데이터 파트({@code ddingconnect-data}) 의 {@code POST /api/data/coffeechat/match}
 * 와 정합. 데이터 파트가 후보 풀 조회·정렬·top N 선택까지 모두 담당하므로
 * 백엔드는 폼 6필드를 1회 호출로 전달하고 {@code top_matches} 의 회원 ID 만 추출한다.
 *
 * <p>흐름:
 * <ol>
 *   <li>요청자 폼({@link MatchingRequest}) → 데이터 파트 스키마({@code year, gpa, major, job, tech_stacks, goal})로 매핑.</li>
 *   <li>{@code POST /api/data/coffeechat/match} 1회 호출 — 데이터 파트가 졸업생 후보 풀 조회·점수 계산·정렬·상위 3 추출.</li>
 *   <li>응답 {@code top_matches} 의 {@code id} 만 추출해 {@code List<Long>} 반환 — 카드 조립은
 *       {@link CandidateProfileAssembler} 가 라이브 DB 로 재조회(데이터 파트 응답의 부가 필드는 무시).</li>
 * </ol>
 *
 * <p>호출 실패·타임아웃·비정상 응답은 모두 {@link ErrorStatus#MATCHING_ALGORITHM_FAILED}.
 */
@Slf4j
@Component
public class MatchingAlgorithmClientImpl implements MatchingAlgorithmClient {

    private static final String MATCH_ENDPOINT_PATH = "/api/data/coffeechat/match";

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    private final RestClient restClient;

    // 생성자가 2개(운영 + 테스트용)라 Spring 이 자동 선택 못해 빈 생성 시 무인자 생성자를 찾다 실패한다.
    // Spring 4.3+ 규칙 — 다중 생성자는 @Autowired 로 운영 생성자를 명시해야 한다.
    @Autowired
    public MatchingAlgorithmClientImpl(@Value("${matching.algorithm.base-url}") String baseUrl) {
        this(buildDefaultRestClient(baseUrl));
    }

    /** 테스트용 — MockRestServiceServer(test scope) 바인딩된 RestClient 주입. */
    MatchingAlgorithmClientImpl(RestClient restClient) {
        this.restClient = restClient;
    }

    private static RestClient buildDefaultRestClient(String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public List<Long> topMatches(MatchingRequest form, Long requesterId) {
        AlgorithmMatchResponse response;
        try {
            response = restClient.post()
                    .uri(MATCH_ENDPOINT_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(AlgorithmMatchRequest.from(form))
                    .retrieve()
                    .body(AlgorithmMatchResponse.class);
        } catch (RestClientException e) {
            log.error("매칭 알고리즘 호출 실패: requesterId={}, {}", requesterId, e.getMessage());
            throw new CoffeeChatHandler(ErrorStatus.MATCHING_ALGORITHM_FAILED);
        }
        if (response == null || response.topMatches() == null) {
            log.error("매칭 알고리즘 응답이 비어 있습니다: requesterId={}", requesterId);
            throw new CoffeeChatHandler(ErrorStatus.MATCHING_ALGORITHM_FAILED);
        }
        return response.topMatches().stream()
                .map(TopMatch::id)
                .toList();
    }

    /**
     * 폼 {@code capability}(콤마 구분 자유 텍스트)를 {@link TechStackName} 화이트리스트 토큰 리스트로 정규화한다.
     * 토큰 trim → 대문자 → {@code TechStackName.valueOf} 매칭 안 되는 항목은 버린다. 중복 제거.
     */
    private static List<String> normalizeTechStacks(String capability) {
        if (capability == null || capability.isBlank()) {
            return List.of();
        }
        return Arrays.stream(capability.split(","))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .map(token -> token.toUpperCase(Locale.ROOT))
                .filter(MatchingAlgorithmClientImpl::isKnownTechStack)
                .distinct()
                .toList();
    }

    private static boolean isKnownTechStack(String name) {
        try {
            TechStackName.valueOf(name);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 알고리즘 요청 본문 — 데이터 파트 {@code RecommendInput} 스키마(6필드 플랫, snake_case)와 정합.
     * {@code year} 는 Pydantic 이 {@code str} 로 받으므로 학년(Integer) → 문자열 변환.
     */
    record AlgorithmMatchRequest(
            String year,
            String gpa,
            String major,
            String job,
            @JsonProperty("tech_stacks") List<String> techStacks,
            String goal
    ) {
        static AlgorithmMatchRequest from(MatchingRequest form) {
            return new AlgorithmMatchRequest(
                    form.grade() != null ? String.valueOf(form.grade()) : "",
                    form.gpa(),
                    form.major(),
                    form.interestedJob(),
                    normalizeTechStacks(form.capability()),
                    form.targetCompany()
            );
        }
    }

    /**
     * 알고리즘 응답의 후보 카드 1장 — 데이터 파트 {@code TopMatchItem} 와 정합.
     * 백엔드는 {@code id} 만 사용하며, 나머지 필드는 {@link CandidateProfileAssembler}
     * 가 DB 로 재조립하므로 응답 본문 파싱 단계에서만 받고 무시한다.
     */
    record TopMatch(
            Long id,
            String name,
            String department,
            String company,
            String job,
            String career,
            String location,
            @JsonProperty("tech_stacks") List<String> techStacks,
            @JsonProperty("match_score") double matchScore
    ) {}

    /** 알고리즘 응답 envelope — {@code status} + {@code top_matches} (이미 정렬된 상위 3장). */
    record AlgorithmMatchResponse(
            String status,
            @JsonProperty("top_matches") List<TopMatch> topMatches
    ) {}
}
