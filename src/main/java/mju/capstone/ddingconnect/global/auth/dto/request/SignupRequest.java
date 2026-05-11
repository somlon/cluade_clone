package mju.capstone.ddingconnect.global.auth.dto.request;

import mju.capstone.ddingconnect.domain.member.domain.MemberRole;

public record SignupRequest(
        String email,
        String password,
        String nickname,
        MemberRole role
        //TODO 재학증명서 or 졸업증명서

) {}
