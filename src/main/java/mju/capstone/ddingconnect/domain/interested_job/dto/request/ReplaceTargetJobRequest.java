package mju.capstone.ddingconnect.domain.interested_job.dto.request;

import mju.capstone.ddingconnect.domain.interested_job.domain.TargetJobCategory;

import java.util.List;

/**
 * [관심 직군 일괄 교체 요청 DTO]
 * @param categories 교체할 관심 직군 전체 리스트. null 이면 400, 빈 리스트는 전부 삭제를 의미한다.
 */
public record ReplaceTargetJobRequest(
        List<TargetJobCategory> categories
) {}
