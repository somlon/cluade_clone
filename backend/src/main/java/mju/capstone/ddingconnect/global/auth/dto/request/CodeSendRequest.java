package mju.capstone.ddingconnect.global.auth.dto.request;

import jakarta.validation.constraints.Pattern;
import mju.capstone.ddingconnect.global.common.ValidationPattern;

public record CodeSendRequest(
    @Pattern(
            regexp = ValidationPattern.MJU_EMAIL_REGEX,
            message = ValidationPattern.MJU_EMAIL_MESSAGE
    )
    String email
){}
