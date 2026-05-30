package mju.capstone.ddingconnect.global.aws.controller;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.global.auth.annotation.LoginMember;
import mju.capstone.ddingconnect.global.aws.S3Service;
import mju.capstone.ddingconnect.global.aws.dto.PresignedUploadRequest;
import mju.capstone.ddingconnect.global.aws.dto.PresignedUploadResponse;
import mju.capstone.ddingconnect.global.response.exception.handler.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [범용 파일 업로드 컨트롤러]
 * 브라우저가 S3 로 직접 PUT 할 수 있는 presigned URL 을 발급한다(명함·포트폴리오·이미지 공통).
 */
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController implements FileSwagger {

    private final S3Service s3Service;

    /** 업로드용 presigned URL 발급 (로그인 필수) */
    @PostMapping("/presigned-url")
    public ApiResponse<PresignedUploadResponse> createPresignedUrl(
            @Parameter(hidden = true) @LoginMember Member member,
            @Valid @RequestBody PresignedUploadRequest request) {
        PresignedUploadResponse response = s3Service.createUploadPresign(
                request.fileName(),
                request.contentType(),
                request.uploadType().allowedContentTypes());
        return ApiResponse.onSuccess(response);
    }
}
