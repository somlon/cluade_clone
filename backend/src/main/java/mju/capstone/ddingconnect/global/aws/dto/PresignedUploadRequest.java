package mju.capstone.ddingconnect.global.aws.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import mju.capstone.ddingconnect.global.aws.UploadType;

/**
 * [presigned 업로드 URL 발급 요청]
 *
 * @param uploadType  업로드 용도(IMAGE | PORTFOLIO) — content-type 화이트리스트 분기 기준
 * @param fileName    원본 파일명(확장자 포함) — S3 저장 키 생성에 사용
 * @param contentType 업로드할 파일의 Content-Type — presign 에 서명되며, 실제 PUT 요청 헤더와 동일해야 한다
 */
public record PresignedUploadRequest(
        @NotNull UploadType uploadType,
        @NotBlank String fileName,
        @NotBlank String contentType
) {}
