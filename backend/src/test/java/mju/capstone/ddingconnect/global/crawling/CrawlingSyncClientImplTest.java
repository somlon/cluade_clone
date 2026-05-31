package mju.capstone.ddingconnect.global.crawling;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("CrawlingSyncClientImpl 단위 테스트")
class CrawlingSyncClientImplTest {

    private static final String ENDPOINT_PATH = "/api/data/crawling/sync";

    private MockRestServiceServer server;
    private CrawlingSyncClientImpl client;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
        client = new CrawlingSyncClientImpl(restClientBuilder.build());
    }

    @Test
    @DisplayName("200 OK 응답 시 예외 없이 완료된다")
    void sync_success() {
        server.expect(requestTo(ENDPOINT_PATH))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess());

        assertThatCode(() -> client.sync()).doesNotThrowAnyException();

        server.verify();
    }

    @Test
    @DisplayName("서버 에러(500) 시 RestClientException 을 전파한다")
    void sync_serverError_throwsRestClientException() {
        server.expect(requestTo(ENDPOINT_PATH))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.sync())
                .isInstanceOf(RestClientException.class);

        server.verify();
    }
}
