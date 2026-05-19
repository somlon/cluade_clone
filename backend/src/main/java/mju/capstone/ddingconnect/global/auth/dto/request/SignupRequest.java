package mju.capstone.ddingconnect.global.auth.dto.request;

import jakarta.validation.constraints.Pattern;
import mju.capstone.ddingconnect.domain.member.domain.MemberRole;
import mju.capstone.ddingconnect.global.common.ValidationPattern;

public record SignupRequest(
        @Pattern(
                regexp = ValidationPattern.MJU_EMAIL_REGEX,
                message = ValidationPattern.MJU_EMAIL_MESSAGE
        )
        String email,
        String password,
        String nickname,
        MemberRole role
        //TODO 재학증명서 or 졸업증명서
) {}
