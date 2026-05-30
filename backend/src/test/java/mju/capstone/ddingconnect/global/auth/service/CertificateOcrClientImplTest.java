package mju.capstone.ddingconnect.global.auth.service;

import mju.capstone.ddingconnect.global.auth.dto.response.CertificateVerifyResponse;
import mju.capstone.ddingconnect.global.auth.dto.response.CertificateVerifyResponse.StudentInfo;
import mju.capstone.ddingconnect.global.response.exception.handler.AuthHandler;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("CertificateOcrClientImpl 단위 테스트")
class CertificateOcrClientImplTest {

    private static final String ENDPOINT_URI = "/api/data/verify";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final Long MEMBER_ID = 7L;

    private MockRestServiceServer server;
    private CertificateOcrClientImpl client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new CertificateOcrClientImpl(builder.build());
    }

    @Test
    @DisplayName("재학생 승인 응답을 파싱한다 — student_info(type/name/department) + grade(문자열)")
    void parsesEnrolledStudentResponse() {
        server.expect(requestTo(ENDPOINT_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(USER_ID_HEADER, String.valueOf(MEMBER_ID)))                 // X-User-Id = member PK
                .andExpect(header(HttpHeaders.CONTENT_TYPE,                                    // multipart relay
                        Matchers.startsWith(MediaType.MULTIPART_FORM_DATA_VALUE)))
                .andRespond(withSuccess(enrolledJson(), MediaType.APPLICATION_JSON));

        CertificateVerifyResponse res = client.verify(pdf(), MEMBER_ID);

        assertThat(res.isApproved()).isTrue();
        assertThat(res.studentInfo().type()).isEqualTo("재학생");
        assertThat(res.studentInfo().name()).isEqualTo("홍길동");
        assertThat(res.studentInfo().department()).isEqualTo("데이터사이언스전공");
        assertThat(res.studentInfo().gradeAsInt()).isEqualTo(4);
        server.verify();
    }

    @Test
    @DisplayName("졸업생 승인 응답은 grade 키가 없어 gradeAsInt 가 null")
    void parsesGraduateResponseWithoutGrade() {
        server.expect(requestTo(ENDPOINT_URI))
                .andRespond(withSuccess(graduateJson(), MediaType.APPLICATION_JSON));

        CertificateVerifyResponse res = client.verify(pdf(), MEMBER_ID);

        assertThat(res.isApproved()).isTrue();
        assertThat(res.studentInfo().type()).isEqualTo("졸업생");
        assertThat(res.studentInfo().gradeAsInt()).isNull();
        server.verify();
    }

    @Test
    @DisplayName("미승인(is_approved=false) 응답은 student_info 없이 그대로 파싱한다(예외 아님, 미사용 필드 무시)")
    void parsesRejectedResponse() {
        server.expect(requestTo(ENDPOINT_URI))
                .andRespond(withSuccess(
                        "{\"status\":\"fail\",\"user_id\":7,\"is_approved\":false,"
                                + "\"message\":\"명지대학교 재학/졸업증명서가 아닙니다.\","
                                + "\"details\":{\"is_myongji\":false,\"is_certificate\":true}}",
                        MediaType.APPLICATION_JSON));

        CertificateVerifyResponse res = client.verify(pdf(), MEMBER_ID);

        assertThat(res.isApproved()).isFalse();
        assertThat(res.studentInfo()).isNull();
        server.verify();
    }

    @Test
    @DisplayName("데이터 파트 호출 실패(500) 시 AuthHandler(CERTIFICATE_OCR_FAILED) 로 변환한다")
    void rethrowsFailureAsAuthHandler() {
        server.expect(requestTo(ENDPOINT_URI)).andRespond(withServerError());

        assertThatThrownBy(() -> client.verify(pdf(), MEMBER_ID))
                .isInstanceOf(AuthHandler.class);
        server.verify();
    }

    @Test
    @DisplayName("StudentInfo.gradeAsInt — 정상 숫자만 파싱하고 null/blank/형식오류는 null")
    void gradeAsIntParsesOnlyValidNumbers() {
        assertThat(new StudentInfo("재학생", "n", "d", "3").gradeAsInt()).isEqualTo(3);
        assertThat(new StudentInfo("재학생", "n", "d", " 2 ").gradeAsInt()).isEqualTo(2);
        assertThat(new StudentInfo("졸업생", "n", "d", null).gradeAsInt()).isNull();
        assertThat(new StudentInfo("재학생", "n", "d", "  ").gradeAsInt()).isNull();
        assertThat(new StudentInfo("재학생", "n", "d", "사학년").gradeAsInt()).isNull();
    }

    // ===== Fixture helpers =====

    private MultipartFile pdf() {
        return new MockMultipartFile(
                "certificate", "cert.pdf", MediaType.APPLICATION_PDF_VALUE,
                "%PDF-1.4 test".getBytes(StandardCharsets.UTF_8));
    }

    /** 데이터 파트 /verify 재학생 승인 응답 (student_info 에 grade 문자열 포함). */
    private String enrolledJson() {
        return "{\"status\":\"success\",\"user_id\":7,\"is_approved\":true,"
                + "\"message\":\"명지대학교 재학생 인증이 완료되었습니다.\","
                + "\"student_info\":{\"type\":\"재학생\",\"name\":\"홍길동\","
                + "\"department\":\"데이터사이언스전공\",\"grade\":\"4\"}}";
    }

    /** 데이터 파트 /verify 졸업생 승인 응답 (grade 키 없음). */
    private String graduateJson() {
        return "{\"status\":\"success\",\"user_id\":7,\"is_approved\":true,"
                + "\"message\":\"명지대학교 졸업생 인증이 완료되었습니다.\","
                + "\"student_info\":{\"type\":\"졸업생\",\"name\":\"김선배\","
                + "\"department\":\"융합소프트웨어학부\"}}";
    }
}
