package mju.capstone.ddingconnect.domain.coffeechat.dto.response;

import mju.capstone.ddingconnect.domain.interested_job.domain.TargetJobCategory;
import mju.capstone.ddingconnect.domain.job_post.dto.response.JobPostResponse;
import mju.capstone.ddingconnect.domain.member.domain.MemberRole;
import mju.capstone.ddingconnect.domain.techstack.domain.TechStackName;

import java.util.List;

/**
 * [커피챗 매칭 후보 상세 응답 DTO]
 * 매칭 상대 상세(화면 3) 및 "나의 활동 › 커피챗"에 노출되는 후보 정보.
 * 카드(MatchedCandidateResponse) 필드 + 상세 전용 필드(포트폴리오/소셜 링크/명함/공고).
 * 한 record에 역할별 전용 필드를 두며, 해당 역할이 아니면 null (MemberResponse 패턴).
 * - GRADUATE 전용: enrollmentYear, company, careerYear, businessCardImage, jobPosts
 * - STUDENT 전용: grade
 * region은 소스 미확정 상태로 항상 null.
 */
public record MatchedCandidateDetailResponse(
        // 공통 필드
        Long memberId,
        MemberRole role,
        String nickname,
        String department,
        List<TargetJobCategory> jobCategories,
        List<TechStackName> techStacks,

        // GRADUATE 전용 — studentNumber에서 파생한 입학연도
        String enrollmentYear,

        // STUDENT 전용
        Integer grade,

        // GRADUATE 전용
        String company,
        Integer careerYear,

        // 소스 미확정 — 항상 null
        String region,

        // 상세 전용 공통 필드
        String portfolio,
        String githubLink,
        String linkedinLink,

        // 상세 전용 GRADUATE 필드
        String businessCardImage,
        List<JobPostResponse> jobPosts
) {}
