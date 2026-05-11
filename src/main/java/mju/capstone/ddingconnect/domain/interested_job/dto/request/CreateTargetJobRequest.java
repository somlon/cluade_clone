package mju.capstone.ddingconnect.domain.interested_job.dto.request;

import mju.capstone.ddingconnect.domain.interested_job.domain.TargetJobCategory;

/**
 * [관심 직군 등록 요청 DTO]
 * @param interestedJob 관심 직군 카테고리 (ENUM)
 */
public record CreateTargetJobRequest(
        TargetJobCategory interestedJob
) {}
