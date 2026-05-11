package mju.capstone.ddingconnect.global.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import mju.capstone.ddingconnect.global.auth.dto.request.LoginRequest;
import mju.capstone.ddingconnect.global.auth.dto.request.SignupRequest;
import mju.capstone.ddingconnect.global.auth.dto.response.TokenResponse;
import mju.capstone.ddingconnect.global.response.exception.handler.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "인증 API", description = "인증 관련 API입니다.")
public interface AuthSwagger {

    @Operation(
            summary = "회원 가입 API",
            description = "회원 가입을 진행합니다. 이메일 인증 성공 이후 호출 해야 합니다 이메일 중복 시 에러 반환합니다."
    )
    @PostMapping("/signup")
    ApiResponse<String> signup(
            @Parameter(description = "회원 가입 정보 (현재는 인증서 사진 없음.. 개발 예정)")
            @RequestBody SignupRequest request);





    @Operation(
            summary = "로그인 API",
            description = "로그인을 진행합니다. 회원 가입 이후 호출합니다. jwt 토큰을 바디로 반환합니다."
    )
    @PostMapping("/login")
    ApiResponse<TokenResponse> login(
            @Parameter(description = "로그인 정보")
            @RequestBody LoginRequest request);

}
