package mju.capstone.ddingconnect.global.jwt;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mju.capstone.ddingconnect.domain.member.domain.Member;

import java.util.Date;
import java.util.Optional;

/**
 * JWT 관련 유틸리티 기능을 정의하는 인터페이스.
 */
public interface JwtUtil {
    /**
     * 회원 정보를 기반으로 액세스 토큰을 생성합니다.
     * @param member 토큰 생성 대상 회원 정보
     * @return 생성된 액세스 토큰 문자열
     */
    String createAccessToken(Member member);


    /**
     * HTTP 요청 헤더에서 액세스 토큰을 추출합니다.
     * @param request HTTP 요청 객체
     * @return 액세스 토큰이 존재하면 Optional에 담아 반환, 없으면 빈 Optional
     */
    Optional<String> extractAccessToken(HttpServletRequest request);


    /**
     * 토큰의 유효성을 검사합니다.
     * (서명 검증, 만료 여부 등)
     * @param token 검사 대상 토큰 문자열
     * @return 유효하면 true, 아니면 false
     */
    boolean isTokenValid(String token);


    Long extractUserId(String token);

}