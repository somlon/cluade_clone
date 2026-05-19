package mju.capstone.ddingconnect.global.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mju.capstone.ddingconnect.domain.member.domain.repository.MemberRepository;
import mju.capstone.ddingconnect.global.auth.domain.CustomUserDetails;
import mju.capstone.ddingconnect.global.jwt.JwtUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final MemberRepository memberRepository;

    private static final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final String[] JWT_WHITELIST = {
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/api/v1/auth/**",
            "/actuator/health"
    };

    /**
     * 매 요청마다 실행되는 진입점.
     * 토큰을 검증·인증하고, 다음 필터로 체인을 이어준다.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (isWhitelisted(request)) {
            log.info("jwt 토큰 인증 제외 URL입니다.");
            // 화이트리스트라면 JWT 인증 로직 수행하지 않고 다음 필터로 진행
            filterChain.doFilter(request, response);
            return;
        }
        checkAccessTokenAndAuthentication(request, response, filterChain);
    }

    private boolean isWhitelisted(HttpServletRequest request) {
        String path = request.getRequestURI();
        for (String pattern : JWT_WHITELIST) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * ① 요청 헤더에서 토큰 추출
     * ② 토큰 유효성·블랙리스트 여부 검사
     * ③ 토큰에서 Google ID 추출
     * ④ Google ID로 DB 조회하여 UserDetails 확보
     * ⑤ 인증 정보(SecurityContext) 저장
     */
    private void checkAccessTokenAndAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        jwtUtil.extractAccessToken(request)
                .filter(jwtUtil::isTokenValid) // 토큰을 검증
                .ifPresent(this::authenticateUser);

        filterChain.doFilter(request, response);
    }

    /**
     * 유저 인증 관련 처리
     * - 토큰에서 Google ID를 추출 / 로그인 시점 추출
     * - DB에서 UserDetails를 조회
     * - 인증 객체를 생성하여 SecurityContextHolder에 저장
     */
    private void authenticateUser(String token) {

        Long userId = jwtUtil.extractUserId(token);
        memberRepository.findById(userId)
//                .filter(m -> m.getStagettus() == MemberStatus.ACTIVE) 추 후 멤버 탈퇴 구현 시 추가
                .map(CustomUserDetails::new)
                .ifPresent(this::saveAuthentication);
    }


    /**
     * 인증 객체를 SecurityContextHolder에 저장한다.
     *
     * @param userDetails 인증에 성공한 사용자 정보
     */
    private void saveAuthentication(CustomUserDetails userDetails) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, Collections.emptyList());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }
}
