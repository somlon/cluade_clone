package mju.capstone.ddingconnect.global.aws;

import lombok.RequiredArgsConstructor;
import mju.capstone.ddingconnect.global.response.exception.handler.S3Handler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import static mju.capstone.ddingconnect.global.response.code.status.ErrorStatus._FILE_DELETE_FAILED;
import static mju.capstone.ddingconnect.global.response.code.status.ErrorStatus._FILE_TOO_LARGE;
import static mju.capstone.ddingconnect.global.response.code.status.ErrorStatus._FILE_TYPE_NOT_ALLOWED;
import static mju.capstone.ddingconnect.global.response.code.status.ErrorStatus._FILE_UPLOAD_FAILED;


/**
 * S3와 연동하여 파일 업로드 및 삭제 기능을 제공하는 서비스 클래스입니다.
 *
 * - {@link #uploadImage(MultipartFile)}: content-type 검증 없는 원본 패턴(회원가입 증명서 흐름이 PDF 도 이 메서드로 업로드 중이므로 유지). 신규 호출처는 가능한 한 {@link #uploadFile} 을 쓴다.
 * - {@link #uploadFile(MultipartFile, Set, long)}: content-type 화이트리스트 + 크기 제한을 진입부에서 검증하는 일반 파일 업로드 — TODO O 신설.
 */
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;


    @Value("${s3.public-bucket}")
    private String publicBucketName;

    @Value("${cloud.aws.region.static}")
    private String region;


    /**
     * MultipartFile 을 S3 버킷에 업로드합니다 (일반 파일 — 이미지/PDF 등 공통).
     * 진입부에서 content-type 화이트리스트와 크기 제한을 검증한 뒤 업로드합니다.
     *
     * @param file               업로드할 파일
     * @param allowedContentTypes 허용 content-type 집합 (예: {@code Set.of("application/pdf")})
     * @param maxBytes           허용 최대 바이트 수
     * @return 업로드된 객체의 public URL
     */
    public String uploadFile(MultipartFile file, Set<String> allowedContentTypes, long maxBytes) {
        validate(file, allowedContentTypes, maxBytes);

        String key = convertToSaveName(file.getOriginalFilename());

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(publicBucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();
            RequestBody requestBody = RequestBody.fromInputStream(file.getInputStream(), file.getSize());
            s3Client.putObject(request, requestBody);
        } catch (IOException e) {
            throw new S3Handler(_FILE_UPLOAD_FAILED);
        }
        return getUrl(key);
    }

    /**
     * 파일 검증 (content-type 화이트리스트 + 크기 제한).
     * - null/빈 파일 → _FILE_UPLOAD_FAILED
     * - content-type 미지원 → _FILE_TYPE_NOT_ALLOWED
     * - 크기 초과 → _FILE_TOO_LARGE
     */
    private void validate(MultipartFile file, Set<String> allowedContentTypes, long maxBytes) {
        if (file == null || file.isEmpty()) {
            throw new S3Handler(_FILE_UPLOAD_FAILED);
        }
        String contentType = file.getContentType();
        if (contentType == null || !allowedContentTypes.contains(contentType)) {
            throw new S3Handler(_FILE_TYPE_NOT_ALLOWED);
        }
        if (file.getSize() > maxBytes) {
            throw new S3Handler(_FILE_TOO_LARGE);
        }
    }

    /**
     * MultipartFile 형태의 이미지를 S3 버킷에 업로드합니다.
     * <b>content-type 검증 없는 원본 패턴</b> — 회원가입 증명서 흐름(`AuthServiceImpl.signup`)이 PDF 도 이 메서드로 업로드 중이므로 유지.
     * 신규 호출처는 가능한 한 {@link #uploadFile(MultipartFile, Set, long)} 을 사용해 content-type 화이트리스트를 명시한다.
     *
     * @param image 업로드할 파일 (이름은 image 이지만 실제로는 PDF 도 통과)
     * @return 업로드된 파일의 public URL
     */
    public String uploadImage(MultipartFile image) {
        if (image == null) throw new S3Handler(_FILE_UPLOAD_FAILED);

        /* 1‑1. S3 Key 생성: "이름‑UUID.확장자" */
        String key = convertToSaveName(image.getOriginalFilename());

        /* 1‑2. PutObjectRequest + RequestBody */
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(publicBucketName)
                    .key(key)
                    .contentType(image.getContentType())
                    .build();

            RequestBody requestBody = RequestBody.fromInputStream(image.getInputStream(), image.getSize());
            /* MultipartFile → InputStream → RequestBody 로 변환 */
            s3Client.putObject(request, requestBody);
        } catch (IOException e) {
            throw new S3Handler(_FILE_UPLOAD_FAILED);
        }
        /* 1‑3. 업로드한 파일의 public URL 반환 */
        return getUrl(key);
    }

    /**
     * 원시 바이트 배열을 지정한 키로 S3 에 업로드한다. (MultipartFile 경로 외, 서버가 생성한 파일용)
     * 로드맵 PDF 다운로드 흐름에서 {@link mju.capstone.ddingconnect.domain.roadmap.service.RoadmapPdfRenderer} 결과를 업로드하는 데 사용한다.
     *
     * @param bytes        업로드할 바이트 배열
     * @param key          저장할 S3 객체 키 (예: "roadmaps/123.pdf")
     * @param contentType  Content-Type (예: "application/pdf")
     * @return 업로드된 객체의 public URL
     */
    public String uploadBytes(byte[] bytes, String key, String contentType) {
        if (bytes == null || bytes.length == 0) {
            throw new S3Handler(_FILE_UPLOAD_FAILED);
        }
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(publicBucketName)
                    .key(key)
                    .contentType(contentType)
                    .build();
            s3Client.putObject(request, RequestBody.fromBytes(bytes));
        } catch (Exception e) {
            throw new S3Handler(_FILE_UPLOAD_FAILED);
        }
        return getUrl(key);
    }

    /**
     * 지정 키에 대한 GET presigned URL 을 발급한다.
     * 다운로드 강제(`Content-Disposition: attachment; filename="..."`)를 위해 응답 헤더 오버라이드를 함께 지정한다.
     *
     * @param key       S3 객체 키
     * @param ttl       URL 유효 기간
     * @param fileName  브라우저 저장 시 사용할 파일명 (특수문자 처리 권장)
     * @return presigned URL 문자열
     */
    public String generatePresignedUrl(String key, Duration ttl, String fileName) {
        // HTTP 헤더(ISO-8859-1)에 한글 등 비ASCII 문자가 들어가지 못하므로 RFC 5987 형식으로 인코딩한다.
        // ASCII 폴백(filename=) + UTF-8 표기(filename*=) 두 가지를 함께 둬 구형 클라이언트와도 호환한다.
        String sanitized = fileName.replace("\"", "").replace("\r", "").replace("\n", "");
        String asciiFallback = sanitized.replaceAll("[^\\x20-\\x7E]", "_");
        String utf8Encoded = URLEncoder.encode(sanitized, StandardCharsets.UTF_8).replace("+", "%20");
        String contentDisposition = "attachment; filename=\"" + asciiFallback + "\"; filename*=UTF-8''" + utf8Encoded;

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(publicBucketName)
                .key(key)
                .responseContentDisposition(contentDisposition)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
        return presigned.url().toString();
    }

    /**
     * S3에 저장된 객체 키로부터 접근 가능한 public URL을 생성하여 반환합니다.
     *
     * @param key S3 객체 키
     * @return S3 객체의 public URL
     */
    public String getUrl(String key) {
        return "https://" + publicBucketName
                + ".s3." + region
                + ".amazonaws.com/" +
                URLEncoder.encode(key, StandardCharsets.UTF_8);
    }

    /**
     * public URL로부터 S3 객체 키를 추출하여 해당 객체 삭제 요청을 보냅니다.
     *
     * @param url 삭제할 객체의 public URL
     * @throws S3Handler 삭제 실패 시 예외 발생
     */
    public void deleteImage(String url) {
        String key = extractKeyFromUrl(url);

        try {
            DeleteObjectRequest delReq = DeleteObjectRequest.builder()
                    .bucket(publicBucketName)
                    .key(key)
                    .build();
            s3Client.deleteObject(delReq);
        } catch (Exception e) {
            throw new S3Handler(_FILE_DELETE_FAILED);
        }
    }


    /**
     * 원본 파일명을 받아 S3에 저장할 고유한 키명으로 변환합니다.
     * (파일명 + UUID + 확장자)
     *
     * @param original 원본 파일명 (ex: image.jpg)
     * @return 변환된 키명 (ex: image-uuid.jpg)
     * @throws S3Handler 확장자가 없으면 예외 발생
     */
    private String convertToSaveName(String original) {
        //확장자 제거
        int idx = original.lastIndexOf('.');
        if (idx < 0) throw new S3Handler(_FILE_UPLOAD_FAILED);
        String name = original.substring(0, idx); // 실제 이름
        String ext  = original.substring(idx); // 확장자
        return String.format("%s-%s%s",name, UUID.randomUUID(), ext);
    }


    /**
     * public URL에서 S3 객체 키를 추출합니다.
     * (버킷명과 URL 접두사를 제거하고 URL 디코딩 처리)
     *
     * @param url public URL
     * @return S3 객체 키
     */
    private String extractKeyFromUrl(String url) {
        String prefix = "https://" + publicBucketName + ".s3." +
                region + ".amazonaws.com/";
        return java.net.URLDecoder.decode(url.replace(prefix, ""), StandardCharsets.UTF_8);
    }


}




