package mju.capstone.ddingconnect.global.aws;

import lombok.RequiredArgsConstructor;
import mju.capstone.ddingconnect.global.response.exception.handler.S3Handler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static mju.capstone.ddingconnect.global.response.code.status.ErrorStatus._FILE_DELETE_FAILED;
import static mju.capstone.ddingconnect.global.response.code.status.ErrorStatus._FILE_UPLOAD_FAILED;


/**
 * S3와 연동하여 파일 업로드 및 삭제 기능을 제공하는 서비스 클래스입니다.
 */
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;


    @Value("${s3.public-bucket}")
    private String publicBucketName;

    @Value("${cloud.aws.region.static}")
    private String region;


    /**
     * MultipartFile 형태의 이미지를 S3 버킷에 업로드합니다.
     *
     * @param image 업로드할 이미지 파일
     * @return 업로드된 이미지의 public URL
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




