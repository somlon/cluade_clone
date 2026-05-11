package mju.capstone.ddingconnect.global.auth.service;

import lombok.RequiredArgsConstructor;
import mju.capstone.ddingconnect.global.auth.dto.request.LoginRequest;
import mju.capstone.ddingconnect.global.auth.dto.request.SignupRequest;
import mju.capstone.ddingconnect.global.auth.dto.response.TokenResponse;
import mju.capstone.ddingconnect.domain.member.domain.Graduate;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.member.domain.MemberRole;
import mju.capstone.ddingconnect.domain.member.domain.Student;
import mju.capstone.ddingconnect.domain.member.domain.repository.GraduateRepository;
import mju.capstone.ddingconnect.domain.member.domain.repository.MemberRepository;
import mju.capstone.ddingconnect.domain.member.domain.repository.StudentRepository;
import mju.capstone.ddingconnect.global.jwt.JwtUtil;
import mju.capstone.ddingconnect.global.response.code.status.ErrorStatus;
import mju.capstone.ddingconnect.global.response.exception.handler.AuthHandler;
import mju.capstone.ddingconnect.global.response.exception.handler.MemberHandler;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final MemberRepository memberRepository;
    private final StudentRepository studentRepository;
    private final GraduateRepository graduateRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public String signup(SignupRequest request) {

        //멤버 역할 검증
        if (request.role() == null) {
            throw new AuthHandler(ErrorStatus.INVALID_ROLE);
        }

        // 이메일 중복 체크
        if (memberRepository.existsByEmail(request.email())) {
            throw new MemberHandler(ErrorStatus.DUPLICATE_EMAIL);
        }

        //TODO 재학증명서 or 졸업증명서 확인


        //새 멤버 생성
        Member member = Member.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .role(request.role())
                .isDeleted(false)
                .build();

        memberRepository.save(member);
        //이어서 롤 기반의 재학생, 졸업생 생성
        createRoleRecord(member, request.role());
        return "회원가입을 성공적으로 완료하였습니다";
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new AuthHandler(ErrorStatus.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new AuthHandler(ErrorStatus.INVALID_CREDENTIALS);
        }

        String accessToken = jwtUtil.createAccessToken(member);
        return new TokenResponse(accessToken);
    }

    private void createRoleRecord(Member member, MemberRole role) {
        if (role == MemberRole.STUDENT) {
            studentRepository.save(Student.builder().member(member).build());
        } else if (role == MemberRole.GRADUATE) {
            graduateRepository.save(Graduate.builder().member(member).build());
        }
    }
}
