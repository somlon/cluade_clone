package mju.capstone.ddingconnect.domain.coffeechat.service;

import lombok.extern.slf4j.Slf4j;
import mju.capstone.ddingconnect.domain.coffeechat.dto.request.MatchingRequest;
import mju.capstone.ddingconnect.global.response.code.status.ErrorStatus;
import mju.capstone.ddingconnect.global.response.exception.handler.CoffeeChatHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;

/**
 * [매칭 알고리즘 클라이언트 구현체 — RestClient]
 *
 * base URL 은 설정값 {@code matching.algorithm.base-url} 에서 주입한다(하드코딩 금지).
 * 호출 실패·타임아웃·비정상 응답은 모두 {@link ErrorStatus#MATCHING_ALGORITHM_FAILED} 로 변환한다.
 *
 * <p><b>타 파트 협의 미확정</b>: 알고리즘 엔드포인트 경로와 요청/응답 JSON 스키마는
 * 아직 확정되지 않았다. 현재는 호출 골격만 두며, 스키마 확정 후
 * {@code MATCH_ENDPOINT_PATH}·{@code AlgorithmMatchRequest}·{@code AlgorithmMatchResponse}
 * 의 매핑만 채우면 된다.
 */
@Slf4j
@Component
public class MatchingAlgorithmClientImpl implements MatchingAlgorithmClient {

    // TODO(타 파트 협의): 알고리즘 매칭 엔드포인트 경로 확정 필요. base URL 뒤에 결합된다.
    private static final String MATCH_ENDPOINT_PATH = "/match";

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    private final RestClient restClient;

    public MatchingAlgorithmClientImpl(@Value("${matching.algorithm.base-url}") String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        this.restClient = RestClient.builder()
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
                    .body(AlgorithmMatchRequest.of(form, requesterId))
                    .retrieve()
                    .body(AlgorithmMatchResponse.class);
        } catch (RestClientException e) {
            // 연결 실패·타임아웃·4xx/5xx 응답·역직렬화 실패는 모두 RestClientException 계열
            log.error("매칭 알고리즘 호출 실패: requesterId={}, {}", requesterId, e.getMessage());
            throw new CoffeeChatHandler(ErrorStatus.MATCHING_ALGORITHM_FAILED);
        }

        if (response == null || response.memberIds() == null) {
            log.error("매칭 알고리즘 응답이 비어 있습니다: requesterId={}", requesterId);
            throw new CoffeeChatHandler(ErrorStatus.MATCHING_ALGORITHM_FAILED);
        }
        return response.memberIds();
    }

    /**
     * 알고리즘 요청 본문 — 폼 6필드 + 신청자 ID.
     * TODO(타 파트 협의): 필드명·구조는 요청 JSON 스키마 확정 시 조정.
     */
    private record AlgorithmMatchRequest(
            Long requesterId,
            Integer grade,
            String gpa,
            String major,
            String interestedJob,
            String capability,
            String targetCompany
    ) {
        static AlgorithmMatchRequest of(MatchingRequest form, Long requesterId) {
            return new AlgorithmMatchRequest(
                    requesterId,
                    form.grade(),
                    form.gpa(),
                    form.major(),
                    form.interestedJob(),
                    form.capability(),
                    form.targetCompany());
        }
    }

    /**
     * 알고리즘 응답 본문 — 후보 회원 ID 리스트.
     * TODO(타 파트 협의): 필드명·구조는 응답 JSON 스키마 확정 시 조정.
     */
    private record AlgorithmMatchResponse(List<Long> memberIds) {}
}
