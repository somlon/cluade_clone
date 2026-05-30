package mju.capstone.ddingconnect.global.auth.service;

import lombok.extern.slf4j.Slf4j;
import mju.capstone.ddingconnect.global.auth.dto.response.CertificateVerifyResponse;
import mju.capstone.ddingconnect.global.response.code.status.ErrorStatus;
import mju.capstone.ddingconnect.global.response.exception.handler.AuthHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;

/**
 * [증명서 OCR 인증 클라이언트 구현체 — RestClient]
 *
 * 데이터 파트({@code ddingconnect-data}) 의 {@code POST /api/data/verify} 와 정합.
 * 회원가입 증명서(PDF) 바이트를 멀티파트 {@code file} 파트로 relay 하고, 저장된 회원 PK 를
 * {@code X-User-Id} 헤더로 전달한다(데이터 파트 {@code get_current_user} 인증). 응답은
 * {@link CertificateVerifyResponse} 로 파싱해 이름·학과·학년 추출값을 반환한다.
 *
 * <p>base-url 은 로드맵 클라이언트와 동일하게 {@code data.base-url}(env {@code DATA_BASE_URL},
 * 기본 {@code http://localhost:8000})을 재사용한다. 호출 실패·타임아웃·HTTP 오류·파일 읽기 실패는
 * 모두 {@link ErrorStatus#CERTIFICATE_OCR_FAILED} 로 변환한다 — 회원가입 흐름은 이 예외를
 * best-effort 로 흡수하므로(가입 자체는 성공) 자동 채움만 생략된다.
 */
@Slf4j
@Component
public class CertificateOcrClientImpl implements CertificateOcrClient {

    private static final String VERIFY_ENDPOINT_PATH = "/api/data/verify";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String FILE_PART_NAME = "file";

    // 데이터 파트가 파일명 확장자(.pdf)를 검사하므로 원본 파일명이 .pdf 가 아니면 이 이름으로 대체한다.
    private static final String PDF_EXTENSION = ".pdf";
    private static final String DEFAULT_FILENAME = "certificate.pdf";

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    // 네이버 클로바 OCR 왕복이 수 초 걸릴 수 있어 매칭 클라이언트(5s)보다 길게 둔다(AI 생성 60s 보단 짧게).
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(15);

    private final RestClient restClient;

    // 생성자가 2개(운영 + 테스트용)라 Spring 이 자동 선택 못해 빈 생성 시 무인자 생성자를 찾다 실패한다.
    // Spring 4.3+ 규칙 — 다중 생성자는 @Autowired 로 운영 생성자를 명시해야 한다.
    @Autowired
    public CertificateOcrClientImpl(@Value("${data.base-url}") String baseUrl) {
        this(buildDefaultRestClient(baseUrl));
    }

    /** 테스트용 — MockRestServiceServer(test scope) 바인딩된 RestClient 주입. */
    CertificateOcrClientImpl(RestClient restClient) {
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
    public CertificateVerifyResponse verify(MultipartFile certificate, Long memberId) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add(FILE_PART_NAME, toPdfResource(certificate));
            return restClient.post()
                    .uri(VERIFY_ENDPOINT_PATH)
                    .header(USER_ID_HEADER, String.valueOf(memberId))
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(CertificateVerifyResponse.class);
        } catch (RestClientException | IOException e) {
            log.warn("증명서 OCR 인증 호출 실패: memberId={}, {}", memberId, e.getMessage());
            throw new AuthHandler(ErrorStatus.CERTIFICATE_OCR_FAILED);
        }
    }

    /**
     * 멀티파트 {@code file} 파트로 보낼 PDF 리소스. 업로드된 증명서 바이트를 그대로 relay 하되,
     * 데이터 파트가 파일명 {@code .pdf} 확장자를 검사하므로 원본 파일명이 {@code .pdf} 가 아니면
     * {@link #DEFAULT_FILENAME} 으로 대체한다(바이트 내용 자체는 변경하지 않음).
     */
    private static Resource toPdfResource(MultipartFile certificate) throws IOException {
        byte[] bytes = certificate.getBytes();
        String filename = resolveFilename(certificate.getOriginalFilename());
        return new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }

    private static String resolveFilename(String original) {
        if (original == null || !original.toLowerCase(Locale.ROOT).endsWith(PDF_EXTENSION)) {
            return DEFAULT_FILENAME;
        }
        return original;
    }
}
