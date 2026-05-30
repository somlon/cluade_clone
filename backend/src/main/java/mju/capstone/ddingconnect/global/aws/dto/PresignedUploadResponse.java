package mju.capstone.ddingconnect.global.aws.dto;

import java.time.LocalDateTime;

/**
 * [presigned 업로드 URL 발급 응답]
 *
 * 프론트는 2-step 으로 사용한다.
 * <ol>
 *   <li>{@code uploadUrl} 로 파일 바이트를 {@code PUT} (헤더 Content-Type 은 요청의 contentType 과 동일).</li>
 *   <li>업로드 성공 후 {@code fileUrl} 을 마이페이지 PATCH 의 string URL 필드(businessCardImage·portfolio 등)에 저장.</li>
 * </ol>
 *
 * @param uploadUrl  S3 presigned PUT URL (TTL 후 만료)
 * @param fileUrl    업로드 완료 후 접근 가능한 최종 public URL
 * @param key        S3 객체 키
 * @param expiresAt  presigned URL 만료 시각
 */
public record PresignedUploadResponse(
        String uploadUrl,
        String fileUrl,
        String key,
        LocalDateTime expiresAt
) {}
