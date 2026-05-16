package mju.capstone.ddingconnect.domain.member.service;

import mju.capstone.ddingconnect.domain.coffeechat.domain.CoffeeChat;
import mju.capstone.ddingconnect.domain.coffeechat.domain.repository.CoffeeChatAlarmRepository;
import mju.capstone.ddingconnect.domain.coffeechat.domain.repository.CoffeeChatRepository;
import mju.capstone.ddingconnect.domain.interested_job.domain.repository.TargetJobRepository;
import mju.capstone.ddingconnect.domain.job_post.domain.GraduateJobPost;
import mju.capstone.ddingconnect.domain.job_post.domain.PostContents;
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
import mju.capstone.ddingconnect.global.response.exception.handler.MemberHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberServiceImpl 단위 테스트")
class MemberServiceImplTest {

    @Mock MemberRepository memberRepository;
    @Mock StudentRepository studentRepository;
    @Mock GraduateRepository graduateRepository;

    // hard delete 캐스케이드 의존성
    @Mock QuestionService questionService;
    @Mock AnswerService answerService;
    @Mock RoadmapService roadmapService;
    @Mock JobPostService jobPostService;
    @Mock QuestionRepository questionRepository;
    @Mock AnswerRepository answerRepository;
    @Mock RoadmapRepository roadmapRepository;
    @Mock GraduateJobPostRepository graduateJobPostRepository;
    @Mock CoffeeChatRepository coffeeChatRepository;
    @Mock CoffeeChatAlarmRepository coffeeChatAlarmRepository;
    @Mock AnswerAlarmRepository answerAlarmRepository;
    @Mock JobAlarmRepository jobAlarmRepository;
    @Mock RoadmapAlarmRepository roadmapAlarmRepository;
    @Mock QuestionLikeRepository questionLikeRepository;
    @Mock AnswerLikeRepository answerLikeRepository;
    @Mock TechStackRepository techStackRepository;
    @Mock TargetJobRepository targetJobRepository;

    @InjectMocks MemberServiceImpl memberService;

    private Member buildStudentMember() {
        return Member.builder().id(1L).email("s@mju.ac.kr").nickname("재학생")
                .role(MemberRole.STUDENT).studentNumber("60201234").department("컴퓨터공학과")
                .point(0L).isDeleted(false).build();
    }

    private Member buildGraduateMember() {
        return Member.builder().id(2L).email("g@mju.ac.kr").nickname("졸업생")
                .role(MemberRole.GRADUATE).studentNumber("60150001").department("컴퓨터공학과")
                .point(100L).isDeleted(false).build();
    }

    private Member buildUnknownMember() {
        return Member.builder().id(3L).email("u@mju.ac.kr").nickname("미정")
                .role(MemberRole.UNKNOWN).point(0L).isDeleted(false).build();
    }

    @Test
    @DisplayName("getMyProfile - 재학생 프로필을 grade와 함께 반환한다")
    void getMyProfile_재학생() {
        Member member = buildStudentMember();
        Student student = Student.builder().id(11L).member(member).grade(3).build();
        when(studentRepository.findByMemberId(1L)).thenReturn(Optional.of(student));

        MemberResponse response = memberService.getMyProfile(member);

        assertThat(response.role()).isEqualTo(MemberRole.STUDENT);
        assertThat(response.grade()).isEqualTo(3);
        assertThat(response.company()).isNull();
    }

    @Test
    @DisplayName("getMyProfile - 졸업생 프로필을 회사/경력과 함께 반환한다")
    void getMyProfile_졸업생() {
        Member member = buildGraduateMember();
        Graduate graduate = Graduate.builder().id(22L).member(member)
                .company("네이버").careerYear(5).businessCardImage("img").build();
        when(graduateRepository.findByMemberId(2L)).thenReturn(Optional.of(graduate));

        MemberResponse response = memberService.getMyProfile(member);

        assertThat(response.role()).isEqualTo(MemberRole.GRADUATE);
        assertThat(response.company()).isEqualTo("네이버");
        assertThat(response.careerYear()).isEqualTo(5);
        assertThat(response.grade()).isNull();
    }

    @Test
    @DisplayName("updateMyProfile - 재학생이 닉네임/학년을 수정한다")
    void updateMyProfile_재학생() {
        Member member = buildStudentMember();
        Student student = Student.builder().id(11L).member(member).grade(3).build();
        UpdateMemberRequest request = new UpdateMemberRequest("새닉네임", null, null,
                null, null, null, null, MemberServiceImpl.MAX_GRADE, null, null, null);

        when(studentRepository.findByMemberId(1L)).thenReturn(Optional.of(student));
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

        MemberResponse response = memberService.updateMyProfile(member, request);

        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getNickname()).isEqualTo("새닉네임");

        ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(studentCaptor.capture());
        assertThat(studentCaptor.getValue().getGrade()).isEqualTo(MemberServiceImpl.MAX_GRADE);

        assertThat(response.nickname()).isEqualTo("새닉네임");
    }

    @Test
    @DisplayName("updateMyProfile - 졸업생이 회사/경력을 수정한다")
    void updateMyProfile_졸업생() {
        Member member = buildGraduateMember();
        Graduate graduate = Graduate.builder().id(22L).member(member)
                .company("네이버").careerYear(5).build();
        UpdateMemberRequest request = new UpdateMemberRequest(null, null, null,
                null, null, null, null, null, "newImg", "카카오", 7);

        when(graduateRepository.findByMemberId(2L)).thenReturn(Optional.of(graduate));
        when(graduateRepository.save(any(Graduate.class))).thenAnswer(inv -> inv.getArgument(0));

        MemberResponse response = memberService.updateMyProfile(member, request);

        ArgumentCaptor<Graduate> grdCaptor = ArgumentCaptor.forClass(Graduate.class);
        verify(graduateRepository).save(grdCaptor.capture());
        assertThat(grdCaptor.getValue().getCompany()).isEqualTo("카카오");
        assertThat(grdCaptor.getValue().getCareerYear()).isEqualTo(7);

        assertThat(response.company()).isEqualTo("카카오");
    }

    @Test
    @DisplayName("updateMyProfile - grade > 4 입력 시 4 로 클램프해서 저장")
    void updateMyProfile_grade_클램프() {
        Member member = buildStudentMember();
        Student student = Student.builder().id(11L).member(member).grade(1).build();
        UpdateMemberRequest request = new UpdateMemberRequest(null, null, null,
                null, null, null, null, MemberServiceImpl.MAX_GRADE + 1, null, null, null);

        when(studentRepository.findByMemberId(1L)).thenReturn(Optional.of(student));
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

        memberService.updateMyProfile(member, request);

        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(captor.capture());
        assertThat(captor.getValue().getGrade()).isEqualTo(MemberServiceImpl.MAX_GRADE);
    }

    @Test
    @DisplayName("updateMyProfile - grade < 1 입력 시 MEMBER_INVALID_GRADE 예외")
    void updateMyProfile_grade_음수_예외() {
        Member member = buildStudentMember();
        Student student = Student.builder().id(11L).member(member).grade(2).build();
        UpdateMemberRequest request = new UpdateMemberRequest(null, null, null,
                null, null, null, null, MemberServiceImpl.MIN_GRADE - 2, null, null, null);

        when(studentRepository.findByMemberId(1L)).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> memberService.updateMyProfile(member, request))
                .isInstanceOf(MemberHandler.class);
    }

    @Test
    @DisplayName("updateMyProfile - grade=0 거부")
    void updateMyProfile_grade_0_예외() {
        Member member = buildStudentMember();
        Student student = Student.builder().id(11L).member(member).grade(2).build();
        UpdateMemberRequest request = new UpdateMemberRequest(null, null, null,
                null, null, null, null, MemberServiceImpl.MIN_GRADE - 1, null, null, null);

        when(studentRepository.findByMemberId(1L)).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> memberService.updateMyProfile(member, request))
                .isInstanceOf(MemberHandler.class);
    }

    @Test
    @DisplayName("updateMyProfile - STUDENT 가 GRADUATE 전용 필드 보내면 MEMBER_FIELD_ROLE_MISMATCH 예외")
    void updateMyProfile_역할필드_불일치_학생_예외() {
        Member member = buildStudentMember();
        UpdateMemberRequest request = new UpdateMemberRequest(null, null, null,
                null, null, null, null, null, null, "카카오", null);

        assertThatThrownBy(() -> memberService.updateMyProfile(member, request))
                .isInstanceOf(MemberHandler.class);
        verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateMyProfile - GRADUATE 가 grade 보내면 MEMBER_FIELD_ROLE_MISMATCH 예외")
    void updateMyProfile_역할필드_불일치_졸업생_예외() {
        Member member = buildGraduateMember();
        UpdateMemberRequest request = new UpdateMemberRequest(null, null, null,
                null, null, null, null, 3, null, null, null);

        assertThatThrownBy(() -> memberService.updateMyProfile(member, request))
                .isInstanceOf(MemberHandler.class);
        verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateMyProfile - UNKNOWN 이 grade 또는 graduate 필드 보내면 예외")
    void updateMyProfile_UNKNOWN_예외() {
        Member member = buildUnknownMember();
        UpdateMemberRequest request = new UpdateMemberRequest(null, null, null,
                null, null, null, null, 3, null, null, null);

        assertThatThrownBy(() -> memberService.updateMyProfile(member, request))
                .isInstanceOf(MemberHandler.class);
    }

    @Test
    @DisplayName("withdraw - STUDENT 가 Q/A/Roadmap/CoffeeChat 모두 보유 상태에서 hard delete")
    void withdraw_STUDENT_정상() {
        Member member = buildStudentMember();

        // 본인 작성 Question 1건
        Question myQuestion = Question.builder().id(101L).member(member).build();
        when(questionRepository.findByMemberId(1L)).thenReturn(List.of(myQuestion));

        // 본인 작성 Answer 1건 (정책상 학생이 답변하면 안되지만 기존 row 가 남아있을 수 있음)
        Answer myAnswer = Answer.builder().id(201L).build();
        when(answerRepository.findByMemberId(1L)).thenReturn(List.of(myAnswer));

        // 본인 작성 Roadmap 1건
        Roadmap myRoadmap = Roadmap.builder().id(301L).build();
        when(roadmapRepository.findByMemberId(1L)).thenReturn(List.of(myRoadmap));

        // CoffeeChat: 본인이 보낸 1건 + 받은 1건
        CoffeeChat sent = CoffeeChat.builder().id(401L).requester(member).build();
        CoffeeChat received = CoffeeChat.builder().id(402L).receiver(member).build();
        when(coffeeChatRepository.findByRequesterId(1L)).thenReturn(List.of(sent));
        when(coffeeChatRepository.findByReceiverId(1L)).thenReturn(List.of(received));

        memberService.withdraw(member);

        // 부모 row 위임
        verify(questionService).delete(member, 101L);
        verify(answerService).delete(member, 201L);
        verify(roadmapService).delete(member, 301L);

        // CoffeeChat 양방향 정리
        verify(coffeeChatAlarmRepository).deleteByCoffeeChatId(401L);
        verify(coffeeChatRepository).delete(sent);
        verify(coffeeChatAlarmRepository).deleteByCoffeeChatId(402L);
        verify(coffeeChatRepository).delete(received);

        // 받은 leaf 알람/좋아요
        verify(answerAlarmRepository).deleteByMemberId(1L);
        verify(jobAlarmRepository).deleteByMemberId(1L);
        verify(roadmapAlarmRepository).deleteByMemberId(1L);
        verify(coffeeChatAlarmRepository).deleteByMemberId(1L);
        verify(questionLikeRepository).deleteByMemberId(1L);
        verify(answerLikeRepository).deleteByMemberId(1L);

        // 본인 소유 단순 row
        verify(techStackRepository).deleteByMemberId(1L);
        verify(targetJobRepository).deleteByMemberId(1L);

        // STUDENT 매핑 row
        verify(studentRepository).deleteByMemberId(1L);
        verify(graduateRepository, never()).deleteByMemberId(any());

        // Member 본체
        verify(memberRepository).delete(member);

        // JobPost 는 STUDENT 라 호출 안 됨
        verify(jobPostService, never()).delete(any(), any());
    }

    @Test
    @DisplayName("withdraw - GRADUATE 가 본인 등록 JobPost 도 함께 삭제")
    void withdraw_GRADUATE_JobPost도_삭제() {
        Member member = buildGraduateMember();
        Graduate graduate = Graduate.builder().id(22L).member(member).build();

        when(questionRepository.findByMemberId(2L)).thenReturn(List.of());
        when(answerRepository.findByMemberId(2L)).thenReturn(List.of());
        when(roadmapRepository.findByMemberId(2L)).thenReturn(List.of());
        when(coffeeChatRepository.findByRequesterId(2L)).thenReturn(List.of());
        when(coffeeChatRepository.findByReceiverId(2L)).thenReturn(List.of());

        // 본인 등록 PostContents 2건
        PostContents p1 = PostContents.builder().id(501L).build();
        PostContents p2 = PostContents.builder().id(502L).build();
        GraduateJobPost m1 = GraduateJobPost.builder().id(601L).graduate(graduate).postContents(p1).build();
        GraduateJobPost m2 = GraduateJobPost.builder().id(602L).graduate(graduate).postContents(p2).build();

        when(graduateRepository.findByMemberId(2L)).thenReturn(Optional.of(graduate));
        when(graduateJobPostRepository.findByGraduateId(22L)).thenReturn(List.of(m1, m2));

        memberService.withdraw(member);

        verify(jobPostService).delete(member, 501L);
        verify(jobPostService).delete(member, 502L);

        verify(graduateRepository).deleteByMemberId(2L);
        verify(studentRepository, never()).deleteByMemberId(any());
        verify(memberRepository).delete(member);
    }

    @Test
    @DisplayName("withdraw - CoffeeChat 수신자만인 경우도 함께 정리한다")
    void withdraw_수신자만인_커피챗도_정리() {
        Member member = buildStudentMember();

        when(questionRepository.findByMemberId(any())).thenReturn(List.of());
        when(answerRepository.findByMemberId(any())).thenReturn(List.of());
        when(roadmapRepository.findByMemberId(any())).thenReturn(List.of());
        when(coffeeChatRepository.findByRequesterId(1L)).thenReturn(List.of());

        CoffeeChat receivedOnly = CoffeeChat.builder().id(701L).receiver(member).build();
        when(coffeeChatRepository.findByReceiverId(1L)).thenReturn(List.of(receivedOnly));

        memberService.withdraw(member);

        verify(coffeeChatAlarmRepository).deleteByCoffeeChatId(701L);
        verify(coffeeChatRepository).delete(receivedOnly);
    }

    @Test
    @DisplayName("withdraw - requester 와 receiver 양쪽으로 잡힌 동일 커피챗은 1번만 처리")
    void withdraw_중복_커피챗_dedup() {
        Member member = buildStudentMember();

        when(questionRepository.findByMemberId(any())).thenReturn(List.of());
        when(answerRepository.findByMemberId(any())).thenReturn(List.of());
        when(roadmapRepository.findByMemberId(any())).thenReturn(List.of());

        CoffeeChat dual = CoffeeChat.builder().id(801L).requester(member).receiver(member).build();
        when(coffeeChatRepository.findByRequesterId(1L)).thenReturn(List.of(dual));
        when(coffeeChatRepository.findByReceiverId(1L)).thenReturn(List.of(dual));

        memberService.withdraw(member);

        verify(coffeeChatAlarmRepository, times(1)).deleteByCoffeeChatId(eq(801L));
        verify(coffeeChatRepository, times(1)).delete(dual);
    }
}
