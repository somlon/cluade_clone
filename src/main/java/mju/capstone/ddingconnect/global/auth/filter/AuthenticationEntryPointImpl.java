package mju.capstone.ddingconnect.global.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 *  로그인 요청을 제외한 다른 요청에서 헤더에 토큰이 없거나,
 * 토큰이 만료되거나, 잘못되었을 때 어떻게 할지 정하는 클래스
 */
@Slf4j
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper = new ObjectMapper();


    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        log.warn("Commerce 실행 - {}", authException.getMessage());
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 상태 코드

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("isSuccess", false);
        responseBody.put("code", "AUTH401");
        responseBody.put("message", authException.getMessage());

        String json = objectMapper.writeValueAsString(responseBody);
        response.getWriter().write(json);


    }
}


