package mju.capstone.ddingconnect.domain.member.dto.response;

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
 * - targetJobs: 관심 직군 (재학생 항목 — 졸업생은 빈 리스트)
 * - jobPosts: 등록한 구직 공고 (졸업생 항목 — 재학생은 빈 리스트)
 */
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
     * @param roadmapCount    본인이 생성한 로드맵 수
     * @param questionCount   본인이 작성한 질문 수
     */
    public record ActivityStats(
            long coffeeChatCount,
            long roadmapCount,
            long questionCount
    ) {}
}
