package mju.capstone.ddingconnect.domain.roadmap.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import mju.capstone.ddingconnect.domain.interested_job.domain.TargetJobCategory;
import mju.capstone.ddingconnect.domain.roadmap.dto.request.CreateRoadmapRequest;
import mju.capstone.ddingconnect.domain.techstack.domain.TechStackName;
import mju.capstone.ddingconnect.global.response.code.status.ErrorStatus;
import mju.capstone.ddingconnect.global.response.exception.handler.RoadmapHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;

/**
 * [로드맵 AI 생성 클라이언트 구현체 — RestClient]
 *
 * 데이터 파트({@code ddingconnect-data}) 의 {@code POST /api/data/generate} 와 정합.
 * 회원 식별자는 {@code member_id} URL 쿼리 파라미터로 전달한다(데이터 파트 시그니처 정합 —
 * {@code X-User-Id} 헤더 아님). 요청 body 는 데이터 파트 {@code RoadmapRequest} 스키마
 * (6필드, snake_case), 응답은 AI 가 생성한 {@code RoadmapResponse} JSON 을 그대로 받아
 * 문자열로 반환한다 — 카드 형식 파싱 없이 백엔드 {@code Roadmap.content} 에 그대로 저장한다.
 *
 * <p>호출 실패·타임아웃·HTTP 오류는 모두 {@link ErrorStatus#ROADMAP_AI_GENERATION_FAILED}.
 */
@Slf4j
@Component
public class RoadmapAiClientImpl implements RoadmapAiClient {

    private static final String GENERATE_ENDPOINT_PATH = "/api/data/generate";
    private static final String MEMBER_ID_PARAM = "member_id";

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    // AI(OpenAI GPT) 생성은 수십 초가 걸릴 수 있어 매칭 클라이언트보다 read 타임아웃을 길게 둔다.
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(60);

    private final RestClient restClient;

    // 생성자가 2개(운영 + 테스트용)라 Spring 이 자동 선택 못해 빈 생성 시 무인자 생성자를 찾다 실패한다.
    // Spring 4.3+ 규칙 — 다중 생성자는 @Autowired 로 운영 생성자를 명시해야 한다.
    @Autowired
    public RoadmapAiClientImpl(@Value("${data.base-url}") String baseUrl) {
        this(buildDefaultRestClient(baseUrl));
    }

    /** 테스트용 — MockRestServiceServer(test scope) 바인딩된 RestClient 주입. */
    RoadmapAiClientImpl(RestClient restClient) {
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
    public String generate(CreateRoadmapRequest form, Long memberId) {
        try {
            return restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path(GENERATE_ENDPOINT_PATH)
                            .queryParam(MEMBER_ID_PARAM, memberId)
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(RoadmapGenerationRequest.from(form))
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            log.error("로드맵 AI 생성 호출 실패: memberId={}, {}", memberId, e.getMessage());
            throw new RoadmapHandler(ErrorStatus.ROADMAP_AI_GENERATION_FAILED);
        }
    }

    /**
     * 로드맵 생성 요청 본문 — 데이터 파트 {@code RoadmapRequest} 스키마(6필드, snake_case)와 정합.
     * {@code targetJob}/{@code currentSkills} enum 은 Jackson 이 enum 명(BACKEND, JAVA 등)으로 직렬화한다.
     */
    record RoadmapGenerationRequest(
            Integer grade,
            Double gpa,
            String major,
            @JsonProperty("target_job") TargetJobCategory targetJob,
            @JsonProperty("current_skills") List<TechStackName> currentSkills,
            @JsonProperty("target_company") String targetCompany
    ) {
        static RoadmapGenerationRequest from(CreateRoadmapRequest form) {
            List<TechStackName> skills = form.currentSkills() != null ? form.currentSkills() : List.of();
            return new RoadmapGenerationRequest(
                    form.grade(),
                    form.gpa(),
                    form.major(),
                    form.targetJob(),
                    skills,
                    form.targetCompany()
            );
        }
    }
}
