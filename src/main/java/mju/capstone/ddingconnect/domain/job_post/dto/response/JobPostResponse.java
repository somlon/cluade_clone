package mju.capstone.ddingconnect.domain.job_post.dto.response;

import mju.capstone.ddingconnect.domain.job_post.domain.CareerType;
import mju.capstone.ddingconnect.domain.job_post.domain.JobType;
import mju.capstone.ddingconnect.domain.job_post.domain.PostContents;

import java.time.LocalDate;

/**
 * [구직 공고 응답 DTO]
 */
public record JobPostResponse(
        Long id,
        String companyName,
        String companyImage,
        String region,
        CareerType careerType,
        JobType jobType,
        String fullLocation,
        LocalDate deadline,
        String detailUrl,
        String preferredLanguage
) {
    public static JobPostResponse from(PostContents postContents) {
        return new JobPostResponse(
                postContents.getId(),
                postContents.getCompanyName(),
                postContents.getCompanyImage(),
                postContents.getRegion(),
                postContents.getCareerType(),
                postContents.getJobType(),
                postContents.getFullLocation(),
                postContents.getDeadline(),
                postContents.getDetailUrl(),
                postContents.getPreferredLanguage()
        );
    }
}
