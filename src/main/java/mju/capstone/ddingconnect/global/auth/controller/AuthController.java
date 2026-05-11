package mju.capstone.ddingconnect.global.auth.controller;

import lombok.RequiredArgsConstructor;
import mju.capstone.ddingconnect.global.auth.dto.request.LoginRequest;
import mju.capstone.ddingconnect.global.auth.dto.request.SignupRequest;
import mju.capstone.ddingconnect.global.auth.dto.response.TokenResponse;
import mju.capstone.ddingconnect.global.auth.service.AuthService;
import mju.capstone.ddingconnect.global.response.exception.handler.ApiResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController implements AuthSwagger {

    private final AuthService authService;

    @PostMapping("/signup")
    public ApiResponse<String > signup(@RequestBody SignupRequest request) {
        return ApiResponse.onSuccess(authService.signup(request));

    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@RequestBody LoginRequest request) {
        return ApiResponse.onSuccess(authService.login(request));
    }
}
