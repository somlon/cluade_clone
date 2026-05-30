package mju.capstone.ddingconnect.global.aws;

import mju.capstone.ddingconnect.global.aws.dto.PresignedUploadResponse;
import mju.capstone.ddingconnect.global.response.code.status.ErrorStatus;
import mju.capstone.ddingconnect.global.response.exception.handler.S3Handler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("S3Service 업로드 presigned 발급 단위 테스트")
class S3ServiceTest {

    private static final String BUCKET = "test-bucket";
    private static final String REGION = "ap-northeast-2";
    private static final Duration TTL = Duration.ofMinutes(5);
    private static final String URL_PREFIX = "https://" + BUCKET + ".s3." + REGION + ".amazonaws.com/";
    private static final String SIGNED_PUT_URL = URL_PREFIX + "card-x.png?X-Amz-Signature=sig";

    @Mock S3Client s3Client;
    @Mock S3Presigner s3Presigner;

    private S3Service s3Service;

    @BeforeEach
    void setUp() {
        s3Service = new S3Service(s3Client, s3Presigner);
        ReflectionTestUtils.setField(s3Service, "publicBucketName", BUCKET);
        ReflectionTestUtils.setField(s3Service, "region", REGION);
        ReflectionTestUtils.setField(s3Service, "uploadPresignTtl", TTL);
    }

    private void stubPresigner() throws Exception {
        PresignedPutObjectRequest presigned = org.mockito.Mockito.mock(PresignedPutObjectRequest.class);
        when(presigned.url()).thenReturn(URI.create(SIGNED_PUT_URL).toURL());
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presigned);
    }

    @Test
    @DisplayName("createUploadPresign - 허용 content-type 이면 presigned PUT URL + 최종 fileUrl(getUrl 규약)·key·expiresAt 반환")
    void createUploadPresignReturnsPresign() throws Exception {
        stubPresigner();

        PresignedUploadResponse result = s3Service.createUploadPresign(
                "card.png", "image/png", UploadType.IMAGE.allowedContentTypes());

        assertThat(result.uploadUrl()).isEqualTo(SIGNED_PUT_URL);
        // 최종 fileUrl 은 getUrl(key) 규약과 일치(쿼리스트링 없는 public URL)
        assertThat(result.fileUrl()).isEqualTo(URL_PREFIX + result.key());
        assertThat(result.fileUrl()).doesNotContain("X-Amz-Signature");
        // 키는 "원본이름-UUID.확장자" 규약
        assertThat(result.key()).startsWith("card-").endsWith(".png");
        // expiresAt = now + ttl (실행 지연 감안 여유 비교)
        assertThat(result.expiresAt()).isAfter(LocalDateTime.now().plus(TTL).minusMinutes(1));
        assertThat(result.expiresAt()).isBefore(LocalDateTime.now().plus(TTL).plusMinutes(1));
    }

    @Test
    @DisplayName("createUploadPresign - 설정된 ttl 이 presign signatureDuration 에 그대로 반영된다")
    void createUploadPresignAppliesTtl() throws Exception {
        stubPresigner();

        s3Service.createUploadPresign("card.png", "image/png", UploadType.IMAGE.allowedContentTypes());

        ArgumentCaptor<PutObjectPresignRequest> captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        org.mockito.Mockito.verify(s3Presigner).presignPutObject(captor.capture());
        assertThat(captor.getValue().signatureDuration()).isEqualTo(TTL);
    }

    @Test
    @DisplayName("createUploadPresign - PORTFOLIO 용도면 application/pdf 를 허용한다")
    void createUploadPresignAllowsPortfolioPdf() throws Exception {
        stubPresigner();

        PresignedUploadResponse result = s3Service.createUploadPresign(
                "resume.pdf", "application/pdf", UploadType.PORTFOLIO.allowedContentTypes());

        assertThat(result.key()).startsWith("resume-").endsWith(".pdf");
        assertThat(result.uploadUrl()).isEqualTo(SIGNED_PUT_URL);
    }

    @Test
    @DisplayName("createUploadPresign - 화이트리스트에 없는 content-type 이면 _FILE_TYPE_NOT_ALLOWED")
    void createUploadPresignRejectsDisallowedContentType() {
        // IMAGE 용도에 application/pdf 를 보냄 → 거부 (presigner 호출 전 단락)
        assertThatThrownBy(() -> s3Service.createUploadPresign(
                "card.pdf", "application/pdf", UploadType.IMAGE.allowedContentTypes()))
                .isInstanceOf(S3Handler.class)
                .hasFieldOrPropertyWithValue("code", ErrorStatus._FILE_TYPE_NOT_ALLOWED);
    }

    @Test
    @DisplayName("createUploadPresign - content-type 이 null 이면 _FILE_TYPE_NOT_ALLOWED")
    void createUploadPresignRejectsNullContentType() {
        assertThatThrownBy(() -> s3Service.createUploadPresign(
                "card.png", null, UploadType.IMAGE.allowedContentTypes()))
                .isInstanceOf(S3Handler.class)
                .hasFieldOrPropertyWithValue("code", ErrorStatus._FILE_TYPE_NOT_ALLOWED);
    }
}
