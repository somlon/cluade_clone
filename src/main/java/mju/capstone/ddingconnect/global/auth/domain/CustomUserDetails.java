package mju.capstone.ddingconnect.global.auth.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class    CustomUserDetails implements UserDetails {

    private final Member member;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Member에 Role 필드가 있다고 가정. enum이면 .name(), 문자열이면 그대로.
        return List.of(new SimpleGrantedAuthority("ROLE_" + member.getRole().name()));
    }

    @Override
    public String getPassword() {
        // 소셜 로그인 + JWT 환경이므로 비밀번호 없음
        return null;
    }

    @Override
    public String getUsername() {
        // Spring Security 내부 식별자. googleId나 email 중 고유한 값.
        return member.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

//    @Override
//    public boolean isEnabled() {  추후 멤버 탈퇴 구현 이후 적용
//        return member.getStatus() == MemberStatus.ACTIVE;
//    }
}