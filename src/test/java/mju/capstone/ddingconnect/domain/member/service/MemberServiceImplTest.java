package mju.capstone.ddingconnect.domain.member.service;

import mju.capstone.ddingconnect.domain.member.domain.Graduate;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.member.domain.MemberRole;
import mju.capstone.ddingconnect.domain.member.domain.Student;
import mju.capstone.ddingconnect.domain.member.domain.repository.GraduateRepository;
import mju.capstone.ddingconnect.domain.member.domain.repository.MemberRepository;
import mju.capstone.ddingconnect.domain.member.domain.repository.StudentRepository;
import mju.capstone.ddingconnect.domain.member.dto.request.UpdateMemberRequest;
import mju.capstone.ddingconnect.domain.member.dto.response.MemberResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberServiceImpl 단위 테스트")
class MemberServiceImplTest {

    @Mock MemberRepository memberRepository;
    @Mock StudentRepository studentRepository;
    @Mock GraduateRepository graduateRepository;
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
                null, null, null, null, 4, null, null, null);

        when(studentRepository.findByMemberId(1L)).thenReturn(Optional.of(student));
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

        MemberResponse response = memberService.updateMyProfile(member, request);

        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getNickname()).isEqualTo("새닉네임");

        ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(studentCaptor.capture());
        assertThat(studentCaptor.getValue().getGrade()).isEqualTo(4);

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
    @DisplayName("withdraw - 회원의 isDeleted를 true로 소프트 삭제한다")
    void withdraw_소프트삭제() {
        Member member = buildStudentMember();

        memberService.withdraw(member);

        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(captor.capture());
        assertThat(captor.getValue().getIsDeleted()).isTrue();
        assertThat(captor.getValue().getEmail()).isEqualTo(member.getEmail());
    }
}
