package mju.capstone.ddingconnect.domain.member.dto.request;

import jakarta.validation.Valid;
import mju.capstone.ddingconnect.domain.job_post.dto.request.CreateJobPostLinkRequest;
import mju.capstone.ddingconnect.domain.techstack.domain.TechStackName;

import java.util.List;

/**
 * [졸업생 마이페이지 통합 수정 요청 DTO]
 * `PATCH /api/v1/members/mypage/graduate` 전용 — GRADUATE 마이페이지 편집 항목 일괄 수정.
 *
 * 부분 수정 규약 — 필드가 null 이면 해당 항목은 변경하지 않는다:
 * - profile:            GRADUATE 프로필 필드(공통 + 명함이미지/직무/회사명/경력). null 이면 프로필 미변경.
 * - techStacks:         교체할 기술 스택 전체 리스트. null 이면 미변경, 빈 리스트는 전부 삭제.
 * - jobPostsToAdd:      추가할 구직 공고(링크 전용). null 이면 추가 없음.
 * - jobPostIdsToDelete: 삭제할 구직 공고 ID. null 이면 삭제 없음.
 *
 * STUDENT 전용 항목(targetJobs)은 정의 자체가 없다.
 */
public record UpdateGraduateMyPageRequest(
        @Valid
        UpdateGraduateProfileRequest profile,
        List<TechStackName> techStacks,
        @Valid
        List<CreateJobPostLinkRequest> jobPostsToAdd,
        List<Long> jobPostIdsToDelete
) {}
