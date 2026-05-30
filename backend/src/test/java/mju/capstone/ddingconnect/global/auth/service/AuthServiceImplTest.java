package mju.capstone.ddingconnect.global.auth.service;

import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.member.domain.MemberRole;
import mju.capstone.ddingconnect.domain.member.domain.Student;
import mju.capstone.ddingconnect.domain.member.domain.repository.GraduateRepository;
import mju.capstone.ddingconnect.domain.member.domain.repository.MemberRepository;
import mju.capstone.ddingconnect.domain.member.domain.repository.StudentRepository;
import mju.capstone.ddingconnect.global.auth.dto.request.SignupRequest;
import mju.capstone.ddingconnect.global.auth.dto.response.CertificateVerifyResponse;
import mju.capstone.ddingconnect.global.aws.S3Service;
import mju.capstone.ddingconnect.global.common.SuccessMessage;
import mju.capstone.ddingconnect.global.jwt.JwtUtil;
import mju.capstone.ddingconnect.global.response.code.status.ErrorStatus;
import mju.capstone.ddingconnect.global.response.exception.handler.AuthHandler;
import mju.capstone.ddingconnect.global.response.exception.handler.MemberHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl 회원가입 OCR 자동 채움 단위 테스트")
class AuthServiceImplTest {

    @Mock MemberRepository memberRepository;
    @Mock StudentRepository studentRepository;
    @Mock GraduateRepository graduateRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @Mock S3Service s3Service;
    @Mock CertificateOcrClient certificateOcrClient;

    @InjectMocks AuthServiceImpl authService;

    private static final Long MEMBER_ID = 42L;
    private static final String CERT_URL = "https://bucket.s3/certificate-uuid.pdf";
    private static final String OCR_NAME = "홍길동";
    private static final String OCR_DEPARTMENT = "데이터사이언스전공";

    @Test
    @DisplayName("재학생 가입 — OCR 이름/학과(공통) + 학년(재학생)을 저장한다")
    void studentSignupSavesNameDepartmentAndGrade() {
        stubCertificateUploadAndMemberSave();
        when(certificateOcrClient.verify(any(MultipartFile.class), eq(MEMBER_ID)))
                .thenReturn(approvedStudent("3"));
        Student student = Student.builder().id(11L).build();
        when(studentRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.of(student));

        String result = authService.signup(signupRequest(MemberRole.STUDENT), pdf());

        assertThat(result).isEqualTo(SuccessMessage.SIGNUP_SUCCESS);
        assertThat(savedMember().getName()).isEqualTo(OCR_NAME);
        assertThat(savedMember().getDepartment()).isEqualTo(OCR_DEPARTMENT);
        assertThat(student.getGrade()).isEqualTo(3);
    }

    @Test
    @DisplayName("졸업생 가입 — OCR 이름/학과만 저장하고 학년은 적용하지 않는다(Student 미조회)")
    void graduateSignupSavesNameDepartmentOnly() {
        stubCertificateUploadAndMemberSave();
        when(certificateOcrClient.verify(any(MultipartFile.class), eq(MEMBER_ID)))
                .thenReturn(approvedGraduate());

        String result = authService.signup(signupRequest(MemberRole.GRADUATE), pdf());

        assertThat(result).isEqualTo(SuccessMessage.SIGNUP_SUCCESS);
        assertThat(savedMember().getName()).isEqualTo("김선배");
        assertThat(savedMember().getDepartment()).isEqualTo("융합소프트웨어학부");
        verify(studentRepository, never()).findByMemberId(any()); // 졸업생은 학년 갱신 없음
    }

    @Test
    @DisplayName("OCR 호출 실패 시에도 가입은 성공하고 이름/학과/학년은 비어 있다(best-effort)")
    void ocrCallFailureStillSucceeds() {
        stubCertificateUploadAndMemberSave();
        when(certificateOcrClient.verify(any(MultipartFile.class), eq(MEMBER_ID)))
                .thenThrow(new AuthHandler(ErrorStatus.CERTIFICATE_OCR_FAILED));

        String result = authService.signup(signupRequest(MemberRole.STUDENT), pdf());

        assertThat(result).isEqualTo(SuccessMessage.SIGNUP_SUCCESS);
        assertThat(savedMember().getName()).isNull();
        assertThat(savedMember().getDepartment()).isNull();
        verify(studentRepository, never()).findByMemberId(any());
    }

    @Test
    @DisplayName("OCR 미승인(is_approved=false) 시에도 가입은 성공하고 이름/학과/학년은 비어 있다")
    void ocrNotApprovedStillSucceeds() {
        stubCertificateUploadAndMemberSave();
        when(certificateOcrClient.verify(any(MultipartFile.class), eq(MEMBER_ID)))
                .thenReturn(rejected());

        String result = authService.signup(signupRequest(MemberRole.STUDENT), pdf());

        assertThat(result).isEqualTo(SuccessMessage.SIGNUP_SUCCESS);
        assertThat(savedMember().getName()).isNull();
        assertThat(savedMember().getDepartment()).isNull();
        verify(studentRepository, never()).findByMemberId(any());
    }

    @Test
    @DisplayName("재학생 학년은 상한(4) 초과 시 4로 클램프된다")
    void studentGradeClampedToMax() {
        stubCertificateUploadAndMemberSave();
        when(certificateOcrClient.verify(any(MultipartFile.class), eq(MEMBER_ID)))
                .thenReturn(approvedStudent("5"));
        Student student = Student.builder().id(11L).build();
        when(studentRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.of(student));

        authService.signup(signupRequest(MemberRole.STUDENT), pdf());

        assertThat(student.getGrade()).isEqualTo(Student.MAX_GRADE);
    }

    @Test
    @DisplayName("재학생 학년 문자열이 숫자가 아니면 학년은 null 로 둔다(파싱 실패 best-effort)")
    void studentGradeUnparseableStaysNull() {
        stubCertificateUploadAndMemberSave();
        when(certificateOcrClient.verify(any(MultipartFile.class), eq(MEMBER_ID)))
                .thenReturn(approvedStudent("사학년"));
        Student student = Student.builder().id(11L).grade(2).build(); // 기존값이 있어도
        when(studentRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.of(student));

        authService.signup(signupRequest(MemberRole.STUDENT), pdf());

        assertThat(student.getGrade()).isNull(); // 파싱 불가 → null 로 정규화
    }

    @Test
    @DisplayName("역할이 null 이면 OCR 호출 전에 INVALID_ROLE 로 거부한다")
    void nullRoleRejectedBeforeOcr() {
        assertThatThrownBy(() -> authService.signup(signupRequest(null), pdf()))
                .isInstanceOf(AuthHandler.class);

        verify(memberRepository, never()).save(any());
        verify(certificateOcrClient, never()).verify(any(), any());
    }

    @Test
    @DisplayName("이메일 중복이면 멤버 저장·OCR 호출 없이 DUPLICATE_EMAIL 로 거부한다")
    void duplicateEmailRejectedBeforeOcr() {
        when(memberRepository.existsByEmail(any())).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(signupRequest(MemberRole.STUDENT), pdf()))
                .isInstanceOf(MemberHandler.class);

        verify(memberRepository, never()).save(any());
        verify(certificateOcrClient, never()).verify(any(), any());
    }

    // ===== Fixtures =====

    /** 증명서 S3 업로드 + 멤버 저장(persist 시 PK 부여 모사) 공통 스텁. */
    private void stubCertificateUploadAndMemberSave() {
        when(s3Service.uploadImage(any())).thenReturn(CERT_URL);
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> {
            Member m = invocation.getArgument(0);
            ReflectionTestUtils.setField(m, "id", MEMBER_ID); // IDENTITY 전략의 INSERT 후 PK 부여 모사
            return m;
        });
    }

    /** signup 이 저장한 Member 를 캡처(동일 인스턴스가 OCR 후 dirty checking 으로 갱신됨). */
    private Member savedMember() {
        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(captor.capture());
        return captor.getValue();
    }

    private SignupRequest signupRequest(MemberRole role) {
        return new SignupRequest("user@mju.ac.kr", "password", "닉네임", role);
    }

    private MultipartFile pdf() {
        return new MockMultipartFile(
                "certificate", "cert.pdf", "application/pdf",
                "%PDF-1.4 test".getBytes(StandardCharsets.UTF_8));
    }

    private CertificateVerifyResponse approvedStudent(String grade) {
        return new CertificateVerifyResponse("success", true,
                new CertificateVerifyResponse.StudentInfo("재학생", OCR_NAME, OCR_DEPARTMENT, grade));
    }

    private CertificateVerifyResponse approvedGraduate() {
        return new CertificateVerifyResponse("success", true,
                new CertificateVerifyResponse.StudentInfo("졸업생", "김선배", "융합소프트웨어학부", null));
    }

    private CertificateVerifyResponse rejected() {
        return new CertificateVerifyResponse("fail", false, null);
    }
}
