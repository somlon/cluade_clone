package mju.capstone.ddingconnect.domain.member.dto.request;

import jakarta.validation.constraints.Pattern;

public record UpdateMemberRequest(

        // ── 공통 필드 (STUDENT / GRADUATE 모두 수정 가능) ──────────
        String nickname,
        String studentNumber,
        String department,

        @Pattern(regexp = GITHUB_URL_REGEX, message = GITHUB_URL_MESSAGE)
        String githubLink,

        @Pattern(regexp = LINKEDIN_URL_REGEX, message = LINKEDIN_URL_MESSAGE)
        String linkedinLink,

        String portfolio,
        String profileImage,

        // ── STUDENT 전용 필드 ──────────────────────────────────────
        Integer grade,          // 학년

        // ── GRADUATE 전용 필드 ────────────────────────────────────
        String businessCardImage,   // 명함이미지
        String company,             // 회사명
        Integer careerYear          // 경력
) {
    private static final String GITHUB_URL_REGEX = "^https?://(www\\.)?github\\.com/.+";
    private static final String GITHUB_URL_MESSAGE = "github.com URL 만 허용됩니다.";
    private static final String LINKEDIN_URL_REGEX = "^https?://(www\\.)?linkedin\\.com/.+";
    private static final String LINKEDIN_URL_MESSAGE = "linkedin.com URL 만 허용됩니다.";
}
