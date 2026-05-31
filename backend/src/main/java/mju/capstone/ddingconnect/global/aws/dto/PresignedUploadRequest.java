package mju.capstone.ddingconnect.global.aws.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * [presigned 업로드 URL 발급 요청]
 *
 * 업로드 용도(프로필 사진·명함·포트폴리오)는 엔드포인트 경로로 구분하므로 요청 본문에는 담지 않는다.
 * 용도별 content-type 화이트리스트는 각 엔드포인트가 {@link mju.capstone.ddingconnect.global.aws.UploadType} 을 참조해 강제한다.
 *
 * @param fileName    원본 파일명(확장자 포함) — S3 저장 키 생성에 사용
 * @param contentType 업로드할 파일의 Content-Type — presign 에 서명되며, 실제 PUT 요청 헤더와 동일해야 한다
 */
public record PresignedUploadRequest(
        @NotBlank String fileName,
        @NotBlank String contentType
) {}
