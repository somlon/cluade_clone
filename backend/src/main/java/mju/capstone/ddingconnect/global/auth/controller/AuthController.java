package mju.capstone.ddingconnect.global.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mju.capstone.ddingconnect.global.auth.dto.request.CodeSendRequest;
import mju.capstone.ddingconnect.global.auth.dto.request.LoginRequest;
import mju.capstone.ddingconnect.global.auth.dto.request.SignupRequest;
import mju.capstone.ddingconnect.global.auth.dto.request.VerifyCodeRequest;
import mju.capstone.ddingconnect.global.auth.dto.response.TokenResponse;
import mju.capstone.ddingconnect.global.auth.service.AuthService;
import mju.capstone.ddingconnect.global.auth.service.EmailService;
import mju.capstone.ddingconnect.global.response.exception.handler.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController implements AuthSwagger {

    private final AuthService authService;
    private final EmailService emailService;

    @PostMapping(value = "/signup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<String> signup(
            @RequestPart("request") SignupRequest request,
            @RequestPart("certificate") MultipartFile certificate) {
        return ApiResponse.onSuccess(authService.signup(request, certificate));
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@RequestBody LoginRequest request) {
        return ApiResponse.onSuccess(authService.login(request));
    }

    @PostMapping("/send-code")
    public ApiResponse<String> sendCode(@RequestBody @Valid CodeSendRequest request) {
        emailService.sendCode(request);
        return ApiResponse.onSuccess("이메일 인증 메일이 전송되었습니다.");
    }

    @PostMapping("/verify-code")
    public ApiResponse<String> verifyCode(@RequestBody @Valid VerifyCodeRequest request) {
        emailService.verifyCode(request);
        return ApiResponse.onSuccess("인증 코드가 일치합니다.");
    }

}