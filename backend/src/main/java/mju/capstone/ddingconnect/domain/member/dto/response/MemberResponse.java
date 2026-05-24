package mju.capstone.ddingconnect.domain.member.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
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
 *
 * @JsonInclude(NON_NULL): 비해당 역할 필드(`from(Member, Student/Graduate)` 정적 팩토리가
 * null 로 채우는 필드)는 응답 JSON 에서 키 자체 제외. `point` 는 마이페이지에선 항상 제외해야 하므로
 * {@link #withoutPoint()} 헬퍼로 명시 null 처리 (다른 엔드포인트는 정적 팩토리 그대로 사용해
 * 값이 채워지면 노출, null 이면 어노테이션이 자동 제외).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
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

    /**
     * 마이페이지 응답용 — point 를 항상 제외 (마이페이지에선 노출하지 않는 정책).
     * 다른 엔드포인트(`GET /me` 등)는 정적 팩토리 결과를 그대로 사용해
     * 향후 point 값이 채워지면 노출된다.
     */
    public MemberResponse withoutPoint() {
        return new MemberResponse(
                id, email, name, nickname, studentNumber, department,
                githubLink, linkedinLink, portfolio, profileImage,
                null, role,
                grade,
                businessCardImage, jobType, company, careerYear
        );
    }
}
