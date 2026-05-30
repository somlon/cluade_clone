package mju.capstone.ddingconnect.global.aws.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.global.auth.annotation.LoginMember;
import mju.capstone.ddingconnect.global.aws.dto.PresignedUploadRequest;
import mju.capstone.ddingconnect.global.aws.dto.PresignedUploadResponse;
import mju.capstone.ddingconnect.global.response.exception.handler.ApiResponse;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "파일 업로드", description = "범용 파일 업로드 컨트롤러 — 브라우저가 S3 로 직접 PUT 할 수 있는 presigned URL 을 발급합니다.")
public interface FileSwagger {

    @Operation(
            summary = "업로드용 presigned URL 발급",
            description = """
                    명함·포트폴리오·이미지 공통 업로드용 presigned PUT URL 을 발급합니다. 로그인된 회원만 호출할 수 있습니다.

                    2-step 플로우:
                    1) 이 API 로 {uploadType, fileName, contentType} 을 보내 {uploadUrl, fileUrl, key, expiresAt} 을 받는다.
                    2) uploadUrl 로 파일 바이트를 PUT 한다 (요청 헤더 Content-Type 은 1번에서 보낸 contentType 과 동일해야 S3 가 수락).
                    3) 업로드 성공 후 fileUrl 을 마이페이지 PATCH 의 businessCardImage·portfolio 등 string 필드에 저장한다.

                    uploadType 별 허용 content-type: IMAGE = image/png·image/jpeg·image/webp, PORTFOLIO = application/pdf.
                    허용되지 않은 content-type 은 400(S3400) 으로 거부됩니다.
                    """
    )
    ApiResponse<PresignedUploadResponse> createPresignedUrl(
            @Parameter(hidden = true) @LoginMember Member member,
            @Valid @RequestBody PresignedUploadRequest request);
}
