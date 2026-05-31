package mju.capstone.ddingconnect.global.crawling;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

@Slf4j
@Component
public class CrawlingSyncClientImpl implements CrawlingSyncClient {

    private static final String SYNC_ENDPOINT_PATH = "/api/data/crawling/sync";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(90);

    private final RestClient restClient;

    @Autowired
    public CrawlingSyncClientImpl(@Value("${data.base-url}") String baseUrl) {
        this(buildDefaultRestClient(baseUrl));
    }

    /** 테스트용 — MockRestServiceServer 바인딩된 RestClient 주입. */
    CrawlingSyncClientImpl(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public void sync() {
        try {
            restClient.post()
                    .uri(SYNC_ENDPOINT_PATH)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("크롤링 싱크 호출 실패: {}", e.getMessage());
            throw e;
        }
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
}
