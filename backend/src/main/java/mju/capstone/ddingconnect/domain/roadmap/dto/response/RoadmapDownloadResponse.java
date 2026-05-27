package mju.capstone.ddingconnect.domain.roadmap.dto.response;

import java.time.LocalDateTime;

/**
 * 로드맵 PDF 다운로드 응답 DTO.
 *
 * 프론트는 {@code fileUrl} 을 받아 {@code <a href download>} 또는 {@code window.location.href} 로
 * 파일을 직접 가져온다. {@code fileUrl} 은 S3 presigned GET URL 로, 짧은 만료(application-s3.yml 의
 * {@code aws.s3.download-presign-ttl}) 가 적용된다.
 *
 * @param fileUrl   S3 presigned URL
 * @param fileName  브라우저 저장 시 파일명 (예: "백엔드 개발자 로드맵.pdf")
 * @param expiresAt URL 만료 시각
 */
public record RoadmapDownloadResponse(
        String fileUrl,
        String fileName,
        LocalDateTime expiresAt
) {}
