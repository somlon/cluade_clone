package mju.capstone.ddingconnect.domain.job_post.dto.request;

import mju.capstone.ddingconnect.domain.job_post.domain.CareerType;
import mju.capstone.ddingconnect.domain.job_post.domain.JobType;

import java.time.LocalDate;
import java.util.List;

/**
 * [구직 공고 수정 요청 DTO]
 */
public record UpdateJobPostRequest(
        String companyImage,
        String region,
        CareerType careerType,
        JobType jobType,
        String country,
        String location,
        String fullLocation,
        LocalDate deadline,
        String detailUrl,
        List<String> preferredLanguages,
        String companyName
) {}
