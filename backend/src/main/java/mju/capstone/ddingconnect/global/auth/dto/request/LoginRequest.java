package mju.capstone.ddingconnect.global.auth.dto.request;

public record LoginRequest(
        String email,
        String password
) {}
