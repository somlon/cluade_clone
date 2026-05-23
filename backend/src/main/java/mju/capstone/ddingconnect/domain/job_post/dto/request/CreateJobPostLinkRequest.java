package mju.capstone.ddingconnect.domain.job_post.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * [구직 공고 링크 전용 등록 요청 DTO]
 * 졸업생이 마이페이지에서 본인 공고를 링크만 입력해 등록할 때 사용.
 * 본 DTO 자체는 TODO L(역할별 마이페이지 분리)에서 선행 신설하며,
 * 신규 엔드포인트 `POST /api/v1/job-posts/link` 및 링크 전용 등록 서비스 메서드는
 * TODO R(졸업생 공고 — 링크 전용 등록) 작업에서 함께 도입한다.
 *
 * 현재 사용처: `UpdateGraduateMyPageRequest.jobPostsToAdd` (`PATCH /api/v1/members/mypage/graduate`).
 */
public record CreateJobPostLinkRequest(
        @NotBlank(message = "공고 링크는 필수입니다.")
        @Pattern(regexp = URL_REGEX, message = URL_MESSAGE)
        String detailUrl
) {
    private static final String URL_REGEX = "^https?://.+";
    private static final String URL_MESSAGE = "http(s):// 로 시작하는 URL 만 허용됩니다.";
}
