package mju.capstone.ddingconnect.global.auth.service;

import mju.capstone.ddingconnect.global.auth.dto.request.LoginRequest;
import mju.capstone.ddingconnect.global.auth.dto.request.SignupRequest;
import mju.capstone.ddingconnect.global.auth.dto.response.TokenResponse;

public interface AuthService {

    String signup(SignupRequest request);

    TokenResponse login(LoginRequest request);
}
