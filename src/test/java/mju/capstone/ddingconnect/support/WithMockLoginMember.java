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

    private WithMockLoginMember() {}

    /** 기본 STUDENT 회원으로 인증 세팅 */
    public static Member loginAsStudent() {
        Member member = Member.builder()
                .id(1L).email("test@mju.ac.kr").nickname("테스터")
                .role(MemberRole.STUDENT).studentNumber("60201234")
                .department("컴퓨터공학과").point(0L).isDeleted(false).build();
        register(member);
        return member;
    }

    /** GRADUATE 회원으로 인증 세팅 */
    public static Member loginAsGraduate() {
        Member member = Member.builder()
                .id(1L).email("grd@mju.ac.kr").nickname("졸업생")
                .role(MemberRole.GRADUATE).studentNumber("60150001")
                .department("컴퓨터공학과").point(0L).isDeleted(false).build();
        register(member);
        return member;
    }

    /** 임의 ID/Role의 회원으로 인증 세팅 */
    public static Member loginAs(Long id, MemberRole role) {
        Member member = Member.builder()
                .id(id).email("u" + id + "@mju.ac.kr").nickname("u" + id)
                .role(role).studentNumber("60200000")
                .department("컴퓨터공학과").point(0L).isDeleted(false).build();
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
