package mju.capstone.ddingconnect.global.aws.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mju.capstone.ddingconnect.global.aws.S3Service;
import mju.capstone.ddingconnect.global.aws.UploadType;
import mju.capstone.ddingconnect.global.aws.dto.PresignedUploadRequest;
import mju.capstone.ddingconnect.global.aws.dto.PresignedUploadResponse;
import mju.capstone.ddingconnect.global.auth.annotation.LoginMemberArgumentResolver;
import mju.capstone.ddingconnect.global.config.WebMvcConfig;
import mju.capstone.ddingconnect.global.response.code.status.ErrorStatus;
import mju.capstone.ddingconnect.global.response.exception.handler.S3Handler;
import mju.capstone.ddingconnect.support.WithMockLoginMember;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FileController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({WebMvcConfig.class, LoginMemberArgumentResolver.class})
@DisplayName("FileController 슬라이스 테스트")
class FileControllerTest {

    private static final String PRESIGN_URL = "/api/v1/files/presigned-url";
    private static final String UPLOAD_URL = "https://bucket.s3.ap-northeast-2.amazonaws.com/card-uuid.png?X-Amz-Signature=sig";
    private static final String FILE_URL = "https://bucket.s3.ap-northeast-2.amazonaws.com/card-uuid.png";
    private static final String KEY = "card-uuid.png";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean S3Service s3Service;

    @BeforeEach
    void setUp() { WithMockLoginMember.loginAsGraduate(); }

    @AfterEach
    void tearDown() { WithMockLoginMember.clear(); }

    @Test
    @DisplayName("POST /api/v1/files/presigned-url - IMAGE 업로드 presigned URL 을 발급한다")
    void createPresignedUrl() throws Exception {
        PresignedUploadResponse fixture = new PresignedUploadResponse(
                UPLOAD_URL, FILE_URL, KEY, LocalDateTime.now().plusMinutes(5));
        given(s3Service.createUploadPresign(eq("card.png"), eq("image/png"), any()))
                .willReturn(fixture);

        mockMvc.perform(post(PRESIGN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PresignedUploadRequest(UploadType.IMAGE, "card.png", "image/png"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.uploadUrl").value(UPLOAD_URL))
                .andExpect(jsonPath("$.result.fileUrl").value(FILE_URL))
                .andExpect(jsonPath("$.result.key").value(KEY))
                .andExpect(jsonPath("$.result.expiresAt").exists());
    }

    @Test
    @DisplayName("POST /api/v1/files/presigned-url - PORTFOLIO 용도도 동일 엔드포인트로 발급된다")
    void createPresignedUrlForPortfolio() throws Exception {
        PresignedUploadResponse fixture = new PresignedUploadResponse(
                UPLOAD_URL, FILE_URL, KEY, LocalDateTime.now().plusMinutes(5));
        given(s3Service.createUploadPresign(eq("resume.pdf"), eq("application/pdf"), any()))
                .willReturn(fixture);

        mockMvc.perform(post(PRESIGN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PresignedUploadRequest(UploadType.PORTFOLIO, "resume.pdf", "application/pdf"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.uploadUrl").value(UPLOAD_URL));
    }

    @Test
    @DisplayName("POST /api/v1/files/presigned-url - 비허용 content-type 은 400(S3400)")
    void rejectsDisallowedContentType() throws Exception {
        given(s3Service.createUploadPresign(any(), any(), any()))
                .willThrow(new S3Handler(ErrorStatus._FILE_TYPE_NOT_ALLOWED));

        mockMvc.perform(post(PRESIGN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PresignedUploadRequest(UploadType.IMAGE, "doc.pdf", "application/pdf"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("S3400"));
    }

    @Test
    @DisplayName("POST /api/v1/files/presigned-url - fileName 공백 등 검증 실패는 400 (서비스 미호출)")
    void rejectsInvalidRequest() throws Exception {
        mockMvc.perform(post(PRESIGN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PresignedUploadRequest(UploadType.IMAGE, "  ", "image/png"))))
                .andExpect(status().isBadRequest());

        verify(s3Service, never()).createUploadPresign(any(), any(), any());
    }
}
