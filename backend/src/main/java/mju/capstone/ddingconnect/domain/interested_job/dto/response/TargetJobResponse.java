package mju.capstone.ddingconnect.domain.interested_job.dto.response;

import mju.capstone.ddingconnect.domain.interested_job.domain.TargetJob;
import mju.capstone.ddingconnect.domain.interested_job.domain.TargetJobCategory;

/**
 * [관심 직군 응답 DTO]
 * @param id 관심 직군 PK
 * @param interestedJob 관심 직군 카테고리
 */
public record TargetJobResponse(
        Long id,
        TargetJobCategory interestedJob
) {
    public static TargetJobResponse from(TargetJob targetJob) {
        return new TargetJobResponse(
                targetJob.getId(),
                targetJob.getInterestedJob()
        );
    }
}
