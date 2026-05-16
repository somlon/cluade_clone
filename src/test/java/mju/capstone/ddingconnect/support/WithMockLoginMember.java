package mju.capstone.ddingconnect.support;

import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.member.domain.MemberRole;
import mju.capstone.ddingconnect.global.auth.domain.CustomUserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * [컨트롤러 테스트 보조 헬퍼]
 * @LoginMember 어노테이션을 통한 회원 주입을 위해
 * SecurityContextHolder에 가짜 인증 정보를 세팅합니다.
 */
public final class WithMockLoginMember {

    /** STUDENT/GRADUATE 헬퍼를 한 테스트에서 같이 써도 충돌하지 않도록 ID 를 분리한다. */
    public static final Long STUDENT_ID = 1L;
    public static final Long GRADUATE_ID = 2L;

    private static final String EMAIL_DOMAIN = "@mju.ac.kr";
    private static final String DEPARTMENT = "컴퓨터공학과";
    private static final String STUDENT_NUMBER = "60200000";

    public static final String STUDENT_EMAIL = "test" + EMAIL_DOMAIN;
    public static final String GRADUATE_EMAIL = "grd" + EMAIL_DOMAIN;

    private WithMockLoginMember() {}

    /** 기본 STUDENT 회원으로 인증 세팅 */
    public static Member loginAsStudent() {
        Member member = Member.builder()
                .id(STUDENT_ID).email(STUDENT_EMAIL).nickname("테스터")
                .role(MemberRole.STUDENT).studentNumber(STUDENT_NUMBER)
                .department(DEPARTMENT).point(0L).isDeleted(false).build();
        register(member);
        return member;
    }

    /** GRADUATE 회원으로 인증 세팅 */
    public static Member loginAsGraduate() {
        Member member = Member.builder()
                .id(GRADUATE_ID).email(GRADUATE_EMAIL).nickname("졸업생")
                .role(MemberRole.GRADUATE).studentNumber(STUDENT_NUMBER)
                .department(DEPARTMENT).point(0L).isDeleted(false).build();
        register(member);
        return member;
    }

    /** 임의 ID/Role의 회원으로 인증 세팅 */
    public static Member loginAs(Long id, MemberRole role) {
        Member member = Member.builder()
                .id(id).email("u" + id + EMAIL_DOMAIN).nickname("u" + id)
                .role(role).studentNumber(STUDENT_NUMBER)
                .department(DEPARTMENT).point(0L).isDeleted(false).build();
        register(member);
        return member;
    }

    private static void register(Member member) {
        CustomUserDetails details = new CustomUserDetails(member);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                details, null, details.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    /** 인증 정보 클리어 */
    public static void clear() {
        SecurityContextHolder.clearContext();
    }
}
