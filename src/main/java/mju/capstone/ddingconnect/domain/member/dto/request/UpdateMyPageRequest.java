package mju.capstone.ddingconnect.domain.member.dto.request;

import jakarta.validation.Valid;
import mju.capstone.ddingconnect.domain.interested_job.domain.TargetJobCategory;
import mju.capstone.ddingconnect.domain.job_post.dto.request.CreateJobPostRequest;
import mju.capstone.ddingconnect.domain.techstack.domain.TechStackName;

import java.util.List;

/**
 * [마이페이지 통합 수정 요청 DTO]
 * 마이페이지의 편집 가능 항목 전체를 '수정 완료' 1회 요청으로 일괄 반영하기 위한 요청.
 * 애그리게이터(MyPageService.updateMyPage)가 항목별로 분해해 각 도메인의 기존 수정 API 에 위임한다.
 *
 * 부분 수정 규약 — 필드가 null 이면 해당 항목은 변경하지 않는다:
 * - profile:            회원 공통 + 역할별 프로필 필드. UpdateMemberRequest 를 그대로 재사용해 중복 정의를 피한다.
 *                       null 이면 프로필 미변경. @Valid 로 UpdateMemberRequest 의 형식 검증이 함께 적용된다.
 * - techStacks:         교체할 기술 스택 전체 리스트. null 이면 미변경, 빈 리스트는 전부 삭제.
 * - targetJobs:         교체할 관심 직군 전체 리스트(재학생 항목). null 이면 미변경, 빈 리스트는 전부 삭제.
 * - jobPostsToAdd:      추가할 구직 공고(졸업생 항목). null 이면 추가 없음. 기존 공고 본문 편집은 범위 밖.
 * - jobPostIdsToDelete: 삭제할 구직 공고 ID(졸업생 항목). null 이면 삭제 없음.
 */
public record UpdateMyPageRequest(
        @Valid
        UpdateMemberRequest profile,
        List<TechStackName> techStacks,
        List<TargetJobCategory> targetJobs,
        List<CreateJobPostRequest> jobPostsToAdd,
        List<Long> jobPostIdsToDelete
) {}
