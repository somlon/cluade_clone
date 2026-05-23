package mju.capstone.ddingconnect.domain.member.dto.request;

import jakarta.validation.constraints.Pattern;
import mju.capstone.ddingconnect.global.common.ValidationPattern;

/**
 * [재학생 마이페이지 프로필 수정 요청 DTO]
 * STUDENT 역할 전용. 공통 9필드 + STUDENT 전용 grade 만 노출한다.
 * GRADUATE 전용 필드(businessCardImage·jobType·company·careerYear)는 정의 자체가 없다.
 *
 * 부분 수정 규약 — 필드가 null 이면 해당 항목은 변경하지 않는다.
 * 형식 검증: 이메일은 @mju.ac.kr 도메인, githubLink/linkedinLink 는 각 도메인 URL.
 */
public record UpdateStudentProfileRequest(

        // ── 공통 필드 (STUDENT / GRADUATE 모두 수정 가능) ──────────
        String name,

        @Pattern(regexp = ValidationPattern.MJU_EMAIL_REGEX, message = ValidationPattern.MJU_EMAIL_MESSAGE)
        String email,

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
        Integer grade
) {
    private static final String GITHUB_URL_REGEX = "^https?://(www\\.)?github\\.com/.+";
    private static final String GITHUB_URL_MESSAGE = "github.com URL 만 허용됩니다.";
    private static final String LINKEDIN_URL_REGEX = "^https?://(www\\.)?linkedin\\.com/.+";
    private static final String LINKEDIN_URL_MESSAGE = "linkedin.com URL 만 허용됩니다.";

    /**
     * 기존 회원 정보 수정 위임(MemberService.updateMyProfile) 을 재사용하기 위한 어댑터.
     * GRADUATE 전용 필드는 모두 null 로 채워, validateRoleFields 가 STUDENT 진입 시 통과하도록 한다.
     */
    public UpdateMemberRequest toUpdateMemberRequest() {
        return new UpdateMemberRequest(
                name, email, nickname, studentNumber, department,
                githubLink, linkedinLink, portfolio, profileImage,
                grade,
                null, null, null, null
        );
    }
}
