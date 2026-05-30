package mju.capstone.ddingconnect.global.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mju.capstone.ddingconnect.global.auth.dto.request.LoginRequest;
import mju.capstone.ddingconnect.global.auth.dto.request.SignupRequest;
import mju.capstone.ddingconnect.global.auth.dto.response.CertificateVerifyResponse;
import mju.capstone.ddingconnect.global.auth.dto.response.TokenResponse;
import mju.capstone.ddingconnect.domain.member.domain.Graduate;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.member.domain.MemberRole;
import mju.capstone.ddingconnect.domain.member.domain.Student;
import mju.capstone.ddingconnect.domain.member.domain.repository.GraduateRepository;
import mju.capstone.ddingconnect.domain.member.domain.repository.MemberRepository;
import mju.capstone.ddingconnect.domain.member.domain.repository.StudentRepository;
import mju.capstone.ddingconnect.global.aws.S3Service;
import mju.capstone.ddingconnect.global.common.SuccessMessage;
import mju.capstone.ddingconnect.global.jwt.JwtUtil;
import mju.capstone.ddingconnect.global.response.code.status.ErrorStatus;
import mju.capstone.ddingconnect.global.response.exception.handler.AuthHandler;
import mju.capstone.ddingconnect.global.response.exception.handler.MemberHandler;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final MemberRepository memberRepository;
    private final StudentRepository studentRepository;
    private final GraduateRepository graduateRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final S3Service s3Service;
    private final CertificateOcrClient certificateOcrClient;

    @Override
    @Transactional
    public String signup(SignupRequest request, MultipartFile certificate) {

        //멤버 역할 검증
        if (request.role() == null) {
            throw new AuthHandler(ErrorStatus.INVALID_ROLE);
        }

        // 이메일 중복 체크
        if (memberRepository.existsByEmail(request.email())) {
            throw new MemberHandler(ErrorStatus.DUPLICATE_EMAIL);
        }

        // 재학증명서 or 졸업증명서 S3 업로드
        String certificateUrl = s3Service.uploadImage(certificate);

        //새 멤버 생성
        Member member = Member.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .role(request.role())
                .certificate(certificateUrl)
                .isDeleted(false)
                .build();

        memberRepository.save(member);
        //이어서 롤 기반의 재학생, 졸업생 생성
        createRoleRecord(member, request.role());

        // 증명서 OCR 추출값(이름·학과·학년) 자동 채움 — best-effort (실패/미승인이어도 가입은 성공)
        applyCertificateOcr(member, certificate);

        return SuccessMessage.SIGNUP_SUCCESS;
    }

    /**
     * 데이터 파트 OCR({@code POST /api/data/verify}) 로 증명서에서 이름·학과(공통)·학년(재학생)을
     * 추출해 자동 채운다. 데이터 파트 인증({@code X-User-Id})이 이미 존재하는 회원을 요구하므로
     * member 를 먼저 저장(PK 확보)한 뒤 그 id 로 호출한다.
     *
     * <p><b>실패 정책 = best-effort</b>: 호출 실패({@link ErrorStatus#CERTIFICATE_OCR_FAILED})·
     * 미승인({@code is_approved=false})이어도 가입 자체는 성공시키고 추출값만 비운다
     * (추후 마이페이지에서 수정 가능). 데이터 파트(8000) 다운 시 가입이 막히지 않도록 한다.
     * member/student 는 가입 트랜잭션 안에서 막 저장된 영속 엔티티라 dirty checking 으로 갱신된다.
     */
    private void applyCertificateOcr(Member member, MultipartFile certificate) {
        CertificateVerifyResponse response;
        try {
            response = certificateOcrClient.verify(certificate, member.getId());
        } catch (RuntimeException e) {
            log.warn("증명서 OCR 자동 채움 생략(호출 실패): memberId={}, {}", member.getId(), e.getMessage());
            return;
        }

        if (response == null || !response.isApproved() || response.studentInfo() == null) {
            return;
        }

        CertificateVerifyResponse.StudentInfo info = response.studentInfo();
        member.applyVerifiedProfile(info.name(), info.department());

        // 학년은 재학생 전용 — 졸업생(Graduate)은 학년 개념이 없어 공통 2필드만 채운다.
        if (member.getRole() == MemberRole.STUDENT) {
            studentRepository.findByMemberId(member.getId())
                    .ifPresent(student -> student.assignGrade(info.gradeAsInt()));
        }
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