package mju.capstone.ddingconnect.global.auth.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import mju.capstone.ddingconnect.global.common.ValidationPattern;

public record VerifyCodeRequest(
        @Pattern(
                regexp = ValidationPattern.MJU_EMAIL_REGEX,
                message = ValidationPattern.MJU_EMAIL_MESSAGE
        )
        String email,
        @NotNull
        String code
) {
}
