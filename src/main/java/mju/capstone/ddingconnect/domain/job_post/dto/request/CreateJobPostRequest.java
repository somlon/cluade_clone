package mju.capstone.ddingconnect.domain.job_post.dto.request;

import mju.capstone.ddingconnect.domain.job_post.domain.CareerType;
import mju.capstone.ddingconnect.domain.job_post.domain.JobType;

import java.time.LocalDate;

/**
 * [구직 공고 등록 요청 DTO]
 */
public record CreateJobPostRequest(
        String companyImage,
        String region,
        CareerType careerType,
        JobType jobType,
        String country,
        String location,
        String fullLocation,
        LocalDate deadline,
        String detailUrl,
        String preferredLanguage,
        String companyName
) {}
