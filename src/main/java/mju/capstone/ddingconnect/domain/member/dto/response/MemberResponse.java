package mju.capstone.ddingconnect.domain.member.dto.response;

import mju.capstone.ddingconnect.domain.job_post.domain.JobType;
import mju.capstone.ddingconnect.domain.member.domain.Graduate;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.member.domain.MemberRole;
import mju.capstone.ddingconnect.domain.member.domain.Student;

/**
 * [회원 응답 DTO]
 * Member 공통 필드 + 역할별 전용 필드 포함
 * - STUDENT 전용: grade
 * - GRADUATE 전용: businessCardImage, jobType, company, careerYear
 */
public record MemberResponse(
        // 공통 필드
        Long id,
        String email,
        String name,
        String nickname,
        String studentNumber,
        String department,
        String githubLink,
        String linkedinLink,
        String portfolio,
        String profileImage,
        Long point,
        MemberRole role,

        // STUDENT 전용
        Integer grade,

        // GRADUATE 전용
        String businessCardImage,
        JobType jobType,
        String company,
        Integer careerYear
) {
    /** STUDENT 회원 응답 생성 */
    public static MemberResponse from(Member member, Student student) {
        return new MemberResponse(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getNickname(),
                member.getStudentNumber(),
                member.getDepartment(),
                member.getGithubLink(),
                member.getLinkedinLink(),
                member.getPortfolio(),
                member.getProfileImage(),
                member.getPoint(),
                member.getRole(),
                student != null ? student.getGrade() : null,
                null,
                null,
                null,
                null
        );
    }

    /** GRADUATE 회원 응답 생성 */
    public static MemberResponse from(Member member, Graduate graduate) {
        return new MemberResponse(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getNickname(),
                member.getStudentNumber(),
                member.getDepartment(),
                member.getGithubLink(),
                member.getLinkedinLink(),
                member.getPortfolio(),
                member.getProfileImage(),
                member.getPoint(),
                member.getRole(),
                null,
                graduate != null ? graduate.getBusinessCardImage() : null,
                graduate != null ? graduate.getJobType() : null,
                graduate != null ? graduate.getCompany() : null,
                graduate != null ? graduate.getCareerYear() : null
        );
    }
}
