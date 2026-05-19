package mju.capstone.ddingconnect.global.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.global.response.exception.handler.JwtHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

@Service
@Slf4j
public class JwtUtilImpl implements JwtUtil {

    /** JWT 서명에 사용할 비밀키 (yml에서 주입) */
    @Value("${jwt.secret}")
    private String secret;

    /** 액세스 토큰 만료 시간 (초 단위) */
    @Value("${jwt.access.expiration}")
    private Long accessExpiration;

    /** HTTP 요청 헤더 이름 (e.g. Authorization) */
    @Value("${jwt.access.header}")
    private String accessHeader;

    /** JWT subject - 토큰 종류 구분용 */
    private static final String ACCESS_TOKEN_SUBJECT = "AccessToken";
    /** JWT 클레임 키 - 회원 PK */
    private static final String USERID_CLAIM = "id";
    /** Authorization 헤더 토큰 접두사 */
    private static final String BEARER = "Bearer ";

    /** 서명 키 (한 번만 생성해서 재사용) */
    private SecretKey key;

    @PostConstruct
    private void init() {
        // HS512는 최소 64바이트 키 필요. yml의 jwt.secret이 64자 이상이어야 함.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String createAccessToken(Member member) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessExpiration * 1000);

        return Jwts.builder()
                .subject(ACCESS_TOKEN_SUBJECT)
                .issuedAt(now)
                .expiration(expiry)
                .claim(USERID_CLAIM, member.getId())
                .signWith(key, Jwts.SIG.HS512)
                .compact();
    }

    @Override
    public Optional<String> extractAccessToken(HttpServletRequest request) {
        String header = request.getHeader(accessHeader);
        if (header == null) {
            throw new JwtHandler("Access Token 이 존재하지 않습니다.");
        }
        if (!header.startsWith(BEARER)) {
            throw new JwtHandler("Access Token 형식이 올바르지 않습니다.");
        }
        return header.substring(BEARER.length()).describeConstable();
    }

    @Override
    public boolean isTokenValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            throw new JwtHandler("만료된 JWT 토큰입니다.");
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtHandler("유효하지 않은 Token입니다.");
        }
    }

    /** 토큰에서 회원 PK 추출 */
    public Long extractUserId(String token) {
        return parseClaims(token).get(USERID_CLAIM, Long.class);
    }


    private Claims parseClaims(String token) {
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return jws.getPayload();
        } catch (ExpiredJwtException e) {
            // 만료된 토큰이라도 클레임은 꺼낼 수 있음 (재발급 로직 등에서 유용)
            return e.getClaims();
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtHandler("유효하지 않은 Token입니다.");
        }
    }
}