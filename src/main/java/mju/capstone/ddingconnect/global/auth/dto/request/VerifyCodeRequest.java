package mju.capstone.ddingconnect.global.auth.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record VerifyCodeRequest(
        @Pattern(
                regexp = "^[a-zA-Z0-9._%+\\-]+@mju\\.ac\\.kr$",
                message = "명지대학교 이메일(@mju.ac.kr)만 사용 가능합니다."
        )
        String email,
        @NotNull
        String code
) {
}
