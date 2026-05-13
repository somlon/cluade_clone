package mju.capstone.ddingconnect.global.auth.service;

import mju.capstone.ddingconnect.global.auth.dto.request.LoginRequest;
import mju.capstone.ddingconnect.global.auth.dto.request.SignupRequest;
import mju.capstone.ddingconnect.global.auth.dto.response.TokenResponse;
import org.springframework.web.multipart.MultipartFile;

public interface AuthService {

    String signup(SignupRequest request, MultipartFile certificate);

    TokenResponse login(LoginRequest request);
}
