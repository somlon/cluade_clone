package mju.capstone.ddingconnect.domain.member.service;

import lombok.RequiredArgsConstructor;
import mju.capstone.ddingconnect.domain.member.domain.Graduate;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.member.domain.MemberRole;
import mju.capstone.ddingconnect.domain.member.domain.Student;
import mju.capstone.ddingconnect.domain.member.domain.repository.GraduateRepository;
import mju.capstone.ddingconnect.domain.member.domain.repository.MemberRepository;
import mju.capstone.ddingconnect.domain.member.domain.repository.StudentRepository;
import mju.capstone.ddingconnect.domain.member.dto.request.UpdateMemberRequest;
import mju.capstone.ddingconnect.domain.member.dto.response.MemberResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final StudentRepository studentRepository;
    private final GraduateRepository graduateRepository;

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

        // 1. Member 공통 필드 수정
        Member updated = Member.builder()
                .id(member.getId())
                .email(member.getEmail())
                .password(member.getPassword())
                .role(member.getRole())
                .isDeleted(member.getIsDeleted())
                .point(member.getPoint())
                .certificate(member.getCertificate())
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
                        .grade(request.grade() != null ? request.grade() : student.getGrade())
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
                        .company(request.company() != null ? request.company() : graduate.getCompany())
                        .careerYear(request.careerYear() != null ? request.careerYear() : graduate.getCareerYear())
                        .build();
                return graduateRepository.save(updatedGraduate);
            }).orElse(null);
            return MemberResponse.from(updated, savedGraduate);
        }
    }

    @Override
    @Transactional
    public void withdraw(Member member) {
        Member withdrawn = Member.builder()
                .id(member.getId())
                .email(member.getEmail())
                .password(member.getPassword())
                .nickname(member.getNickname())
                .role(member.getRole())
                .isDeleted(true)
                .studentNumber(member.getStudentNumber())
                .department(member.getDepartment())
                .githubLink(member.getGithubLink())
                .linkedinLink(member.getLinkedinLink())
                .portfolio(member.getPortfolio())
                .profileImage(member.getProfileImage())
                .point(member.getPoint())
                .certificate(member.getCertificate())
                .build();

        memberRepository.save(withdrawn);
    }
}
