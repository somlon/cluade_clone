package mju.capstone.ddingconnect.global.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public class SignupMultipartSchema {

    @Schema(implementation = SignupRequest.class, description = "회원 가입 정보 (JSON)")
    public SignupRequest request;

    @Schema(type = "string", format = "binary", description = "재학/졸업 인증서 이미지")
    public Object certificate;
}
