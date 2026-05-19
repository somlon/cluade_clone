package mju.capstone.ddingconnect.global.auth.service;

import jakarta.validation.Valid;
import mju.capstone.ddingconnect.global.auth.dto.request.CodeSendRequest;
import mju.capstone.ddingconnect.global.auth.dto.request.VerifyCodeRequest;

public interface EmailService {
    void sendCode(@Valid CodeSendRequest request);

    void verifyCode(@Valid VerifyCodeRequest request);
}
