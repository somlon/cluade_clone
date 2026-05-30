package mju.capstone.ddingconnect.domain.coffeechat.service;

import lombok.RequiredArgsConstructor;
import mju.capstone.ddingconnect.domain.coffeechat.dto.response.MatchedCandidateDetailResponse;
import mju.capstone.ddingconnect.domain.coffeechat.dto.response.MatchedCandidateResponse;
import mju.capstone.ddingconnect.domain.coffeechat.dto.response.MyActivityCoffeeChatResponse;
import mju.capstone.ddingconnect.domain.interested_job.domain.TargetJob;
import mju.capstone.ddingconnect.domain.interested_job.domain.TargetJobCategory;
import mju.capstone.ddingconnect.domain.interested_job.domain.repository.TargetJobRepository;
import mju.capstone.ddingconnect.domain.job_post.domain.GraduateJobPost;
import mju.capstone.ddingconnect.domain.job_post.domain.repository.GraduateJobPostRepository;
import mju.capstone.ddingconnect.domain.job_post.dto.response.JobPostResponse;
import mju.capstone.ddingconnect.domain.member.domain.Graduate;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.member.domain.MemberRole;
import mju.capstone.ddingconnect.domain.member.domain.Student;
import mju.capstone.ddingconnect.domain.member.domain.repository.GraduateRepository;
import mju.capstone.ddingconnect.domain.member.domain.repository.MemberRepository;
import mju.capstone.ddingconnect.domain.member.domain.repository.StudentRepository;
import mju.capstone.ddingconnect.domain.techstack.domain.TechStack;
import mju.capstone.ddingconnect.domain.techstack.domain.TechStackName;
import mju.capstone.ddingconnect.domain.techstack.domain.repository.TechStackRepository;
import mju.capstone.ddingconnect.global.response.code.status.ErrorStatus;
import mju.capstone.ddingconnect.global.response.exception.handler.MemberHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * [커피챗 매칭 후보 프로필 조립기]
 *
 * 후보 회원 ID 하나로 회원 공통 정보 + 역할별 부가 정보를 모아
 * 매칭 후보 응답 DTO 를 조립한다. 매칭 결과 카드(화면 2)·매칭 상대 상세(화면 3)·
 * "나의 활동 › 커피챗" 이 공유하는 조회 전용 조립 로직이다.
 *
 * <p>역할별 노출 규칙(MemberResponse 패턴 — 해당 역할이 아니면 null):
 * <ul>
 *   <li>GRADUATE — enrollmentYear(학번 파생) · company · careerYear,
 *       상세는 businessCardImage · 공고 리스트 추가</li>
 *   <li>STUDENT — grade</li>
 * </ul>
 * 직군·기술스택은 역할 무관 공통 필드로 0개면 빈 리스트.
 *
 * <p>커피챗 도메인이 회원·기술스택·직군·공고 등 타 도메인 레포지토리를 직접 주입하는 것은
 * 조회 전용 조립 목적이며 CLAUDE.md QnA 선례상 허용된다.
 */
@Component
@RequiredArgsConstructor
public class CandidateProfileAssembler {

    // 입학연도는 학번의 3·4번째 자리(0-index 2~3) 2자리 — 예: "60221234" → "22"
    private static final int ENROLLMENT_YEAR_BEGIN_INDEX = 2;
    private static final int ENROLLMENT_YEAR_END_INDEX = 4;

    private final MemberRepository memberRepository;
    private final GraduateRepository graduateRepository;
    private final StudentRepository studentRepository;
    private final TechStackRepository techStackRepository;
    private final TargetJobRepository targetJobRepository;
    private final GraduateJobPostRepository graduateJobPostRepository;

    /**
     * 매칭 결과 카드(화면 2)에 노출할 후보 정보를 조립한다.
     *
     * @param memberId 후보 회원 ID
     * @return 후보 카드 응답
     * @throws MemberHandler 회원이 존재하지 않으면 MEMBER_NOT_FOUND
     */
    @Transactional(readOnly = true)
    public MatchedCandidateResponse assembleCard(Long memberId) {
        Member member = findMember(memberId);
        Graduate graduate = findGraduate(member);
        Student student = findStudent(member);

        return new MatchedCandidateResponse(
                member.getId(),
                member.getRole(),
                member.getNickname(),
                member.getDepartment(),
                jobCategoriesOf(memberId),
                techStackNamesOf(memberId),
                graduate != null ? enrollmentYearOf(member.getStudentNumber()) : null,
                student != null ? student.getGrade() : null,
                graduate != null ? graduate.getCompany() : null,
                graduate != null ? graduate.getCareerYear() : null
        );
    }

    /**
     * 매칭 상대 상세(화면 3) 및 "나의 활동 › 커피챗"에 노출할 후보 정보를 조립한다.
     *
     * @param memberId 후보 회원 ID
     * @return 후보 상세 응답
     * @throws MemberHandler 회원이 존재하지 않으면 MEMBER_NOT_FOUND
     */
    @Transactional(readOnly = true)
    public MatchedCandidateDetailResponse assembleDetail(Long memberId) {
        Member member = findMember(memberId);
        Graduate graduate = findGraduate(member);
        Student student = findStudent(member);

        return new MatchedCandidateDetailResponse(
                member.getId(),
                member.getRole(),
                member.getNickname(),
                member.getDepartment(),
                jobCategoriesOf(memberId),
                techStackNamesOf(memberId),
                graduate != null ? enrollmentYearOf(member.getStudentNumber()) : null,
                student != null ? student.getGrade() : null,
                graduate != null ? graduate.getCompany() : null,
                graduate != null ? graduate.getCareerYear() : null,
                member.getPortfolio(),
                member.getGithubLink(),
                member.getLinkedinLink(),
                graduate != null ? graduate.getBusinessCardImage() : null,
                graduate != null ? jobPostsOf(graduate) : null
        );
    }

    /**
     * "나의 활동 › 커피챗" 카드(경량 8필드)를 조립한다.
     * getMyActivity 의 상대(receiver)는 항상 졸업생이지만, 안전 가드로 GRADUATE 가 아니면
     * 졸업생 부가 필드(enrollmentYear · company · jobType · careerYear)는 null 로 둔다.
     *
     * @param memberId 후보(커피챗 상대) 회원 ID
     * @return 나의 활동 커피챗 카드 응답
     * @throws MemberHandler 회원이 존재하지 않으면 MEMBER_NOT_FOUND
     */
    @Transactional(readOnly = true)
    public MyActivityCoffeeChatResponse assembleMyActivityCard(Long memberId) {
        Member member = findMember(memberId);
        Graduate graduate = findGraduate(member);

        return new MyActivityCoffeeChatResponse(
                member.getId(),
                member.getNickname(),
                member.getDepartment(),
                graduate != null ? enrollmentYearOf(member.getStudentNumber()) : null,
                graduate != null ? graduate.getCompany() : null,
                graduate != null ? graduate.getJobType() : null,
                graduate != null ? graduate.getCareerYear() : null,
                techStackNamesOf(memberId)
        );
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));
    }

    /** GRADUATE 회원의 Graduate row. 그 외 역할이거나 매핑이 없으면 null. */
    private Graduate findGraduate(Member member) {
        if (member.getRole() != MemberRole.GRADUATE) {
            return null;
        }
        return graduateRepository.findByMemberId(member.getId()).orElse(null);
    }

    /** STUDENT 회원의 Student row. 그 외 역할이거나 매핑이 없으면 null. */
    private Student findStudent(Member member) {
        if (member.getRole() != MemberRole.STUDENT) {
            return null;
        }
        return studentRepository.findByMemberId(member.getId()).orElse(null);
    }

    private List<TargetJobCategory> jobCategoriesOf(Long memberId) {
        return targetJobRepository.findByMemberId(memberId).stream()
                .map(TargetJob::getInterestedJob)
                .toList();
    }

    private List<TechStackName> techStackNamesOf(Long memberId) {
        return techStackRepository.findByMemberId(memberId).stream()
                .map(TechStack::getName)
                .toList();
    }

    private List<JobPostResponse> jobPostsOf(Graduate graduate) {
        return graduateJobPostRepository.findByGraduateId(graduate.getId()).stream()
                .map(GraduateJobPost::getPostContents)
                .map(JobPostResponse::from)
                .toList();
    }

    /**
     * 학번 문자열에서 입학연도 2자리를 파생한다.
     * 학번이 null 이거나 4자리 미만이면 null (studentNumber 는 nullable 컬럼).
     */
    private String enrollmentYearOf(String studentNumber) {
        if (studentNumber == null || studentNumber.length() < ENROLLMENT_YEAR_END_INDEX) {
            return null;
        }
        return studentNumber.substring(ENROLLMENT_YEAR_BEGIN_INDEX, ENROLLMENT_YEAR_END_INDEX);
    }
}
