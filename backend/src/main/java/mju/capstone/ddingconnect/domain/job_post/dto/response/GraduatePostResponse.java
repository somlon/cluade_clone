package mju.capstone.ddingconnect.domain.job_post.dto.response;

import mju.capstone.ddingconnect.domain.job_post.domain.CareerType;
import mju.capstone.ddingconnect.domain.job_post.domain.GraduateJobPost;
import mju.capstone.ddingconnect.domain.job_post.domain.JobType;
import mju.capstone.ddingconnect.domain.job_post.domain.PostContents;

import java.time.LocalDate;
import java.util.List;

/**
 * [선배 공고 응답 DTO]
 *
 * 구직 정보 화면의 "선배가 올린 공고" 섹션 카드용. 일반 공고(`JobPostResponse`) 의 모든 필드에
 * 등록자(졸업생) 의 닉네임/학과/직무/경력을 추가로 노출한다. 일반 공고 응답과 분리한 이유는
 * 카드에 노출되는 졸업생 부가 정보가 일반 공고에는 없기 때문.
 */
public record GraduatePostResponse(
        // PostContents 본문
        Long id,
        String companyName,
        String companyImage,
        String region,
        CareerType careerType,
        JobType jobType,
        String fullLocation,
        LocalDate deadline,
        String detailUrl,
        List<String> preferredLanguages,

        // 등록자(선배) 정보 — Graduate.member + Graduate
        Long graduateMemberId,
        String graduateNickname,
        String graduateDepartment,
        JobType graduateJobType,
        Integer graduateCareerYear
) {
    /**
     * GraduateJobPost 매핑으로부터 카드 응답을 조립한다.
     */
    public static GraduatePostResponse from(GraduateJobPost mapping) {
        PostContents p = mapping.getPostContents();
        var graduate = mapping.getGraduate();
        var graduateMember = graduate.getMember();
        return new GraduatePostResponse(
                p.getId(), p.getCompanyName(), p.getCompanyImage(), p.getRegion(),
                p.getCareerType(), p.getJobType(), p.getFullLocation(), p.getDeadline(),
                p.getDetailUrl(), p.getPreferredLanguages(),
                graduateMember != null ? graduateMember.getId() : null,
                graduateMember != null ? graduateMember.getNickname() : null,
                graduateMember != null ? graduateMember.getDepartment() : null,
                graduate.getJobType(),
                graduate.getCareerYear()
        );
    }
}
