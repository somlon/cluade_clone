package mju.capstone.ddingconnect.domain.member.dto.request;

import jakarta.validation.Valid;
import mju.capstone.ddingconnect.domain.interested_job.domain.TargetJobCategory;
import mju.capstone.ddingconnect.domain.techstack.domain.TechStackName;

import java.util.List;

/**
 * [재학생 마이페이지 통합 수정 요청 DTO]
 * `PATCH /api/v1/members/mypage/student` 전용 — STUDENT 마이페이지 편집 항목 일괄 수정.
 *
 * 부분 수정 규약 — 필드가 null 이면 해당 항목은 변경하지 않는다:
 * - profile:     STUDENT 프로필 필드(공통 + grade). null 이면 프로필 미변경.
 * - techStacks:  교체할 기술 스택 전체 리스트. null 이면 미변경, 빈 리스트는 전부 삭제.
 * - targetJobs:  교체할 관심 직군 전체 리스트. null 이면 미변경, 빈 리스트는 전부 삭제.
 *
 * GRADUATE 전용 항목(jobPostsToAdd·jobPostIdsToDelete)은 정의 자체가 없다.
 */
public record UpdateStudentMyPageRequest(
        @Valid
        UpdateStudentProfileRequest profile,
        List<TechStackName> techStacks,
        List<TargetJobCategory> targetJobs
) {}
