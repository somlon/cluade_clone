package mju.capstone.ddingconnect.domain.coffeechat.dto.response;

import mju.capstone.ddingconnect.domain.interested_job.domain.TargetJobCategory;
import mju.capstone.ddingconnect.domain.member.domain.MemberRole;
import mju.capstone.ddingconnect.domain.techstack.domain.TechStackName;

import java.util.List;

/**
 * [커피챗 매칭 후보 카드 응답 DTO]
 * 매칭 결과 리스트(화면 2) 카드에 노출되는 후보 정보.
 * 한 record에 역할별 전용 필드를 두며, 해당 역할이 아니면 null (MemberResponse 패턴).
 * - GRADUATE 전용: enrollmentYear, company, careerYear
 * - STUDENT 전용: grade
 * region은 소스 미확정 상태로 항상 null.
 */
public record MatchedCandidateResponse(
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
        String region
) {}
