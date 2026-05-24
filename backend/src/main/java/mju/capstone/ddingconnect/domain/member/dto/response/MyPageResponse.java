package mju.capstone.ddingconnect.domain.member.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import mju.capstone.ddingconnect.domain.interested_job.dto.response.TargetJobResponse;
import mju.capstone.ddingconnect.domain.job_post.dto.response.JobPostResponse;
import mju.capstone.ddingconnect.domain.techstack.dto.response.TechStackResponse;

import java.util.List;

/**
 * [마이페이지 조회 응답 DTO]
 * 마이페이지 화면을 1회 호출로 렌더링하기 위한 통합 응답.
 * - profile: 회원 공통 정보 + 역할별 정보 (MemberResponse 재사용)
 * - activity: 프로필 카드 활동 통계 (커피챗/로드맵/질문 수)
 * - techStacks: 기술 스택 (재학생/졸업생 공통)
 * - targetJobs: 관심 직군 (STUDENT 전용 — GRADUATE 는 null → 응답에서 키 자체 제외)
 * - jobPosts: 등록한 구직 공고 (GRADUATE 전용 — STUDENT 는 null → 응답에서 키 자체 제외)
 *
 * 의미 분리: null = 비해당 필드(키 자체 제외), [] = 해당 역할 + 0개(키 유지).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MyPageResponse(
        MemberResponse profile,
        ActivityStats activity,
        List<TechStackResponse> techStacks,
        List<TargetJobResponse> targetJobs,
        List<JobPostResponse> jobPosts
) {

    /**
     * 프로필 카드의 활동 통계.
     *
     * @param coffeeChatCount 본인이 요청자/수신자로 참여한 수락(ACCEPTED) 커피챗 수
     * @param roadmapCount    본인이 생성한 로드맵 수 (STUDENT 전용 — GRADUATE 는 null → 응답 제외)
     * @param questionCount   본인이 작성한 질문 수
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ActivityStats(
            long coffeeChatCount,
            Long roadmapCount,
            long questionCount
    ) {}
}
