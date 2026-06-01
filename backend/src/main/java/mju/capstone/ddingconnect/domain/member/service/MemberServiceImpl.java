package mju.capstone.ddingconnect.domain.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mju.capstone.ddingconnect.domain.coffeechat.domain.CoffeeChat;
import mju.capstone.ddingconnect.domain.coffeechat.domain.repository.CoffeeChatAlarmRepository;
import mju.capstone.ddingconnect.domain.coffeechat.domain.repository.CoffeeChatRepository;
import mju.capstone.ddingconnect.domain.interested_job.domain.repository.TargetJobRepository;
import mju.capstone.ddingconnect.domain.job_post.domain.GraduateJobPost;
import mju.capstone.ddingconnect.domain.job_post.domain.repository.GraduateJobPostRepository;
import mju.capstone.ddingconnect.domain.job_post.domain.repository.JobAlarmRepository;
import mju.capstone.ddingconnect.domain.job_post.service.JobPostService;
import mju.capstone.ddingconnect.domain.member.domain.Graduate;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.member.domain.MemberRole;
import mju.capstone.ddingconnect.domain.member.domain.Student;
import mju.capstone.ddingconnect.domain.member.domain.repository.GraduateRepository;
import mju.capstone.ddingconnect.domain.member.domain.repository.MemberRepository;
import mju.capstone.ddingconnect.domain.member.domain.repository.StudentRepository;
import mju.capstone.ddingconnect.domain.member.dto.request.UpdateMemberRequest;
import mju.capstone.ddingconnect.domain.member.dto.response.MemberResponse;
import mju.capstone.ddingconnect.domain.qna.answer.domain.Answer;
import mju.capstone.ddingconnect.domain.qna.answer.domain.repository.AnswerAlarmRepository;
import mju.capstone.ddingconnect.domain.qna.answer.domain.repository.AnswerLikeRepository;
import mju.capstone.ddingconnect.domain.qna.answer.domain.repository.AnswerRepository;
import mju.capstone.ddingconnect.domain.qna.answer.service.AnswerService;
import mju.capstone.ddingconnect.domain.qna.question.domain.Question;
import mju.capstone.ddingconnect.domain.qna.question.domain.repository.QuestionLikeRepository;
import mju.capstone.ddingconnect.domain.qna.question.domain.repository.QuestionRepository;
import mju.capstone.ddingconnect.domain.qna.question.service.QuestionService;
import mju.capstone.ddingconnect.domain.roadmap.domain.Roadmap;
import mju.capstone.ddingconnect.domain.roadmap.domain.repository.RoadmapAlarmRepository;
import mju.capstone.ddingconnect.domain.roadmap.domain.repository.RoadmapRepository;
import mju.capstone.ddingconnect.domain.roadmap.service.RoadmapService;
import mju.capstone.ddingconnect.domain.techstack.domain.repository.TechStackRepository;
import mju.capstone.ddingconnect.global.aws.S3Service;
import mju.capstone.ddingconnect.global.aws.UploadType;
import mju.capstone.ddingconnect.global.aws.dto.PresignedUploadRequest;
import mju.capstone.ddingconnect.global.aws.dto.PresignedUploadResponse;
import mju.capstone.ddingconnect.global.response.code.status.ErrorStatus;
import mju.capstone.ddingconnect.global.response.exception.handler.MemberHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    // 학년 허용 범위 (sanitizeGrade) — 단일 정의는 Student 엔티티가 보유, 여기선 재참조(테스트도 이 상수 참조)
    static final int MIN_GRADE = Student.MIN_GRADE;
    static final int MAX_GRADE = Student.MAX_GRADE;

    // 프로필 사진·명함·포트폴리오 업로드 content-type 화이트리스트는 UploadType(IMAGE/PORTFOLIO) 단일 정의를 참조한다.
    // presigned PUT 경로라 서버측 크기 강제는 불가 — 크기 제한은 프론트 클라이언트단에서 검증한다.

    private final MemberRepository memberRepository;
    private final StudentRepository studentRepository;
    private final GraduateRepository graduateRepository;
    private final S3Service s3Service;

    // 회원 hard delete 캐스케이드용 의존성
    private final QuestionService questionService;
    private final AnswerService answerService;
    private final RoadmapService roadmapService;
    private final JobPostService jobPostService;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final RoadmapRepository roadmapRepository;
    private final GraduateJobPostRepository graduateJobPostRepository;
    private final CoffeeChatRepository coffeeChatRepository;
    private final CoffeeChatAlarmRepository coffeeChatAlarmRepository;
    private final AnswerAlarmRepository answerAlarmRepository;
    private final JobAlarmRepository jobAlarmRepository;
    private final RoadmapAlarmRepository roadmapAlarmRepository;
    private final QuestionLikeRepository questionLikeRepository;
    private final AnswerLikeRepository answerLikeRepository;
    private final TechStackRepository techStackRepository;
    private final TargetJobRepository targetJobRepository;

    @Override
    @Transactional(readOnly = true)
    public MemberResponse getMyProfile(Member member) {
        if (member.getRole() == MemberRole.STUDENT) {
            Student student = studentRepository.findByMemberId(member.getId()).orElse(null);
            return MemberResponse.from(member, student);
        } else {
            Graduate graduate = graduateRepository.findByMemberId(member.getId()).orElse(null);
            return MemberResponse.from(member, graduate);
        }
    }

    @Override
    @Transactional
    public MemberResponse updateMyProfile(Member member, UpdateMemberRequest request) {

        // 0. 본인 역할과 일치하지 않는 필드가 채워져 있는지 검증
        validateRoleFields(member.getRole(), request);

        // 1. Member 공통 필드 수정
        Member updated = Member.builder()
                .id(member.getId())
                .email(resolveEmail(member, request))
                .password(member.getPassword())
                .role(member.getRole())
                .isDeleted(member.getIsDeleted())
                .point(member.getPoint())
                .certificate(member.getCertificate())
                .name(request.name() != null ? request.name() : member.getName())
                .nickname(request.nickname() != null ? request.nickname() : member.getNickname())
                .studentNumber(request.studentNumber() != null ? request.studentNumber() : member.getStudentNumber())
                .department(request.department() != null ? request.department() : member.getDepartment())
                .githubLink(request.githubLink() != null ? request.githubLink() : member.getGithubLink())
                .linkedinLink(request.linkedinLink() != null ? request.linkedinLink() : member.getLinkedinLink())
                .portfolio(request.portfolio() != null ? request.portfolio() : member.getPortfolio())
                .profileImage(request.profileImage() != null ? request.profileImage() : member.getProfileImage())
                .build();

        memberRepository.save(updated);

        // 2. 역할에 따라 Student 또는 Graduate 전용 필드 수정
        if (member.getRole() == MemberRole.STUDENT) {
            Student savedStudent = studentRepository.findByMemberId(member.getId()).map(student -> {
                Student updatedStudent = Student.builder()
                        .id(student.getId())
                        .member(student.getMember())
                        .grade(sanitizeGrade(request.grade(), student.getGrade()))
                        .build();
                return studentRepository.save(updatedStudent);
            }).orElse(null);
            return MemberResponse.from(updated, savedStudent);

        } else {
            Graduate savedGraduate = graduateRepository.findByMemberId(member.getId()).map(graduate -> {
                Graduate updatedGraduate = Graduate.builder()
                        .id(graduate.getId())
                        .member(graduate.getMember())
                        .businessCardImage(request.businessCardImage() != null ? request.businessCardImage() : graduate.getBusinessCardImage())
                        .jobType(request.jobType() != null ? request.jobType() : graduate.getJobType())
                        .company(request.company() != null ? request.company() : graduate.getCompany())
                        .careerYear(request.careerYear() != null ? request.careerYear() : graduate.getCareerYear())
                        .build();
                return graduateRepository.save(updatedGraduate);
            }).orElse(null);
            return MemberResponse.from(updated, savedGraduate);
        }
    }

    /**
     * 프로필 사진 업로드용 presigned PUT URL 발급.
     * 발급만 수행한다(저장 안 함) — 프론트가 받은 uploadUrl 로 S3 에 직접 PUT 한 뒤 fileUrl 을 {@code PATCH /me} 의 profileImage 에 저장한다.
     */
    @Override
    public PresignedUploadResponse createProfileImageUploadUrl(PresignedUploadRequest request) {
        return s3Service.createUploadPresign(
                request.fileName(), request.contentType(), UploadType.IMAGE.allowedContentTypes());
    }

    /**
     * 졸업생 명함 사진 업로드용 presigned PUT URL 발급 (GRADUATE 전용).
     * GRADUATE 가 아니면 MEMBER_FIELD_ROLE_MISMATCH (STUDENT/UNKNOWN 모두 거부). 발급만 수행한다(저장 안 함).
     */
    @Override
    public PresignedUploadResponse createBusinessCardUploadUrl(Member member, PresignedUploadRequest request) {
        if (member.getRole() != MemberRole.GRADUATE) {
            throw new MemberHandler(ErrorStatus.MEMBER_FIELD_ROLE_MISMATCH);
        }
        return s3Service.createUploadPresign(
                request.fileName(), request.contentType(), UploadType.IMAGE.allowedContentTypes());
    }

    /**
     * 포트폴리오 파일 업로드용 presigned PUT URL 발급.
     * 발급만 수행한다(저장 안 함) — fileUrl 을 {@code PATCH /me} 의 portfolio 에 저장한다.
     */
    @Override
    public PresignedUploadResponse createPortfolioUploadUrl(PresignedUploadRequest request) {
        return s3Service.createUploadPresign(
                request.fileName(), request.contentType(), UploadType.PORTFOLIO.allowedContentTypes());
    }

    /**
     * 프로필 사진 삭제 — S3 객체 best-effort 삭제 + Member.profileImage = null.
     * 이미 비어 있으면 no-op 으로 현재 프로필을 그대로 반환(idempotent, 200).
     */
    @Override
    @Transactional
    public MemberResponse deleteProfileImage(Member member) {
        String currentUrl = member.getProfileImage();
        if (currentUrl == null || currentUrl.isBlank()) {
            return buildMemberResponse(member);
        }
        safeDeleteFromS3(currentUrl);
        Member updated = rebuildMemberWith(member, null, member.getPortfolio());
        memberRepository.save(updated);
        return buildMemberResponse(updated);
    }

    /**
     * 졸업생 명함 사진 삭제 (GRADUATE 전용) — S3 객체 best-effort 삭제 + Graduate.businessCardImage = null.
     * STUDENT/UNKNOWN 호출 시 MEMBER_FIELD_ROLE_MISMATCH 로 거부 (presigned 발급과 동일 가드).
     */
    @Override
    @Transactional
    public MemberResponse deleteBusinessCard(Member member) {
        if (member.getRole() != MemberRole.GRADUATE) {
            throw new MemberHandler(ErrorStatus.MEMBER_FIELD_ROLE_MISMATCH);
        }
        Graduate graduate = graduateRepository.findByMemberId(member.getId())
                .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));
        String currentUrl = graduate.getBusinessCardImage();
        if (currentUrl == null || currentUrl.isBlank()) {
            return MemberResponse.from(member, graduate);
        }
        safeDeleteFromS3(currentUrl);
        Graduate updated = Graduate.builder()
                .id(graduate.getId())
                .member(graduate.getMember())
                .businessCardImage(null)
                .jobType(graduate.getJobType())
                .company(graduate.getCompany())
                .careerYear(graduate.getCareerYear())
                .build();
        Graduate saved = graduateRepository.save(updated);
        return MemberResponse.from(member, saved);
    }

    /**
     * 포트폴리오 삭제 — S3 객체 best-effort 삭제 + Member.portfolio = null.
     */
    @Override
    @Transactional
    public MemberResponse deletePortfolio(Member member) {
        String currentUrl = member.getPortfolio();
        if (currentUrl == null || currentUrl.isBlank()) {
            return buildMemberResponse(member);
        }
        safeDeleteFromS3(currentUrl);
        Member updated = rebuildMemberWith(member, member.getProfileImage(), null);
        memberRepository.save(updated);
        return buildMemberResponse(updated);
    }

    /**
     * Member 엔티티를 portfolio·profileImage 만 교체해 재구성.
     * 다른 필드는 모두 기존값 유지(부분 수정 패턴 — `updateMyProfile` 의 빌더 reconstruction 과 동일).
     */
    private Member rebuildMemberWith(Member member, String profileImage, String portfolio) {
        return Member.builder()
                .id(member.getId())
                .email(member.getEmail())
                .password(member.getPassword())
                .role(member.getRole())
                .isDeleted(member.getIsDeleted())
                .point(member.getPoint())
                .certificate(member.getCertificate())
                .name(member.getName())
                .nickname(member.getNickname())
                .studentNumber(member.getStudentNumber())
                .department(member.getDepartment())
                .githubLink(member.getGithubLink())
                .linkedinLink(member.getLinkedinLink())
                .portfolio(portfolio)
                .profileImage(profileImage)
                .build();
    }

    /**
     * 역할별 부가 정보(Student/Graduate)를 조회해 MemberResponse 로 조립.
     * `getMyProfile` 과 동일 로직이지만 같은 트랜잭션 내부 사용을 위한 내부 헬퍼로 분리.
     */
    private MemberResponse buildMemberResponse(Member member) {
        if (member.getRole() == MemberRole.STUDENT) {
            Student student = studentRepository.findByMemberId(member.getId()).orElse(null);
            return MemberResponse.from(member, student);
        }
        if (member.getRole() == MemberRole.GRADUATE) {
            Graduate graduate = graduateRepository.findByMemberId(member.getId()).orElse(null);
            return MemberResponse.from(member, graduate);
        }
        return MemberResponse.from(member, (Student) null);
    }

    /**
     * S3 객체 삭제(best-effort) — 실패는 로그만 남기고 트랜잭션에 영향 없음.
     * dangling S3 객체보다 사용자 자산 DB 클리어를 우선시한다(RoadmapServiceImpl.delete 와 동일 패턴).
     */
    private void safeDeleteFromS3(String url) {
        try {
            s3Service.deleteImage(url);
        } catch (Exception e) {
            log.warn("S3 객체 삭제 실패: {} — DB 클리어는 그대로 진행", url, e);
        }
    }

    /**
     * Student.grade 입력값 정제:
     * - null → 기존값 유지
     * - < 1 → 400 (MEMBER_INVALID_GRADE)
     * - > 4 → 4 로 클램프 (저장)
     */
    private Integer sanitizeGrade(Integer raw, Integer fallback) {
        if (raw == null) return fallback;
        if (raw < MIN_GRADE) throw new MemberHandler(ErrorStatus.MEMBER_INVALID_GRADE);
        return Math.min(raw, MAX_GRADE);
    }

    /**
     * 단일 UpdateMemberRequest 에 STUDENT/GRADUATE 양쪽 필드가 모두 존재하므로,
     * 본인 역할과 무관한 필드가 채워져 있으면 400 으로 거부 (silent ignore 방지).
     */
    private void validateRoleFields(MemberRole role, UpdateMemberRequest req) {
        boolean studentField = req.grade() != null;
        boolean graduateField = req.businessCardImage() != null
                || req.jobType() != null
                || req.company() != null
                || req.careerYear() != null;
        if (role == MemberRole.STUDENT && graduateField) {
            throw new MemberHandler(ErrorStatus.MEMBER_FIELD_ROLE_MISMATCH);
        }
        if (role == MemberRole.GRADUATE && studentField) {
            throw new MemberHandler(ErrorStatus.MEMBER_FIELD_ROLE_MISMATCH);
        }
        if (role == MemberRole.UNKNOWN && (studentField || graduateField)) {
            throw new MemberHandler(ErrorStatus.MEMBER_FIELD_ROLE_MISMATCH);
        }
    }

    /**
     * 이메일 수정 정제:
     * - null 또는 기존값과 동일 → 기존값 유지 (변경 없음)
     * - 변경값이면 본인 제외 중복 검사 후, 이미 사용 중이면 409 (DUPLICATE_EMAIL)
     * 형식(@mju.ac.kr) 검증은 컨트롤러 @Valid + UpdateMemberRequest 의 @Pattern 이 담당.
     * (이메일 변경 시 재인증은 TODO D 인증 방식 확정 후 별도 도입 예정.)
     */
    private String resolveEmail(Member member, UpdateMemberRequest request) {
        String requested = request.email();
        if (requested == null || requested.equals(member.getEmail())) {
            return member.getEmail();
        }
        if (memberRepository.existsByEmail(requested)) {
            throw new MemberHandler(ErrorStatus.DUPLICATE_EMAIL);
        }
        return requested;
    }

    /**
     * 회원 탈퇴 = hard delete.
     *
     * 회원이 작성/소유/수신한 모든 자식 행을 단일 @Transactional 안에서 정리한 뒤
     * Member row 자체를 삭제한다.
     *
     * 위임 순서:
     *  (1) 본인 작성 부모 row 들을 각 도메인 delete() 로 캐스케이드 위임
     *      - CoffeeChat: receiver 케이스도 정리하기 위해 service.delete 우회 (요청자 검증 회피)
     *      - Question/Answer/Roadmap: service.delete 위임 (손자까지 캐스케이드)
     *      - GRADUATE 면 본인이 등록한 JobPost 들도 service.delete 위임
     *  (2) 본인 수신 leaf 알람·좋아요 deleteByMemberId
     *  (3) TechStack/TargetJob 본인 row
     *  (4) Student/Graduate 매핑 row
     *  (5) Member row 자체
     */
    @Override
    @Transactional
    public void withdraw(Member member) {
        Long memberId = member.getId();

        // (1-a) CoffeeChat: requester/receiver 양쪽 모두 정리 (service.delete 우회)
        Set<Long> processedCoffeeChatIds = new HashSet<>();
        List<CoffeeChat> sent = coffeeChatRepository.findByRequesterId(memberId);
        List<CoffeeChat> received = coffeeChatRepository.findByReceiverId(memberId);
        List<CoffeeChat> allCoffeeChats = new ArrayList<>();
        allCoffeeChats.addAll(sent);
        allCoffeeChats.addAll(received);
        for (CoffeeChat cc : allCoffeeChats) {
            if (!processedCoffeeChatIds.add(cc.getId())) continue;
            coffeeChatAlarmRepository.deleteByCoffeeChatId(cc.getId());
            coffeeChatRepository.delete(cc);
        }

        // (1-b) Question 본인 작성분 → service.delete (손자 알람/좋아요까지 캐스케이드)
        List<Question> myQuestions = questionRepository.findByMemberId(memberId);
        for (Question q : myQuestions) {
            questionService.delete(member, q.getId());
        }

        // (1-c) Answer 본인 작성분
        List<Answer> myAnswers = answerRepository.findByMemberId(memberId);
        for (Answer a : myAnswers) {
            answerService.delete(member, a.getId());
        }

        // (1-d) Roadmap 본인 작성분
        List<Roadmap> myRoadmaps = roadmapRepository.findByMemberId(memberId);
        for (Roadmap r : myRoadmaps) {
            roadmapService.delete(member, r.getId());
        }

        // (1-e) GRADUATE 면 본인이 등록한 JobPost 들도 service.delete
        if (member.getRole() == MemberRole.GRADUATE) {
            graduateRepository.findByMemberId(memberId).ifPresent(graduate -> {
                List<GraduateJobPost> mappings = graduateJobPostRepository.findByGraduateId(graduate.getId());
                Set<Long> postIds = new HashSet<>();
                for (GraduateJobPost mapping : mappings) {
                    postIds.add(mapping.getPostContents().getId());
                }
                for (Long postId : postIds) {
                    jobPostService.delete(member, postId);
                }
            });
        }

        // (2) 회원이 수신한 leaf 알람·좋아요 정리
        answerAlarmRepository.deleteByMemberId(memberId);
        jobAlarmRepository.deleteByMemberId(memberId);
        roadmapAlarmRepository.deleteByMemberId(memberId);
        coffeeChatAlarmRepository.deleteByMemberId(memberId);
        questionLikeRepository.deleteByMemberId(memberId);
        answerLikeRepository.deleteByMemberId(memberId);

        // (3) 본인 소유 단순 row
        techStackRepository.deleteByMemberId(memberId);
        targetJobRepository.deleteByMemberId(memberId);

        // (4) Student / Graduate 매핑 row
        if (member.getRole() == MemberRole.STUDENT) {
            studentRepository.deleteByMemberId(memberId);
        } else if (member.getRole() == MemberRole.GRADUATE) {
            graduateRepository.deleteByMemberId(memberId);
        }

        // (5) Member row 자체 hard delete
        memberRepository.delete(member);
    }
}
