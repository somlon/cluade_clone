package mju.capstone.ddingconnect.global.response.exception.handler;


import org.springframework.security.core.AuthenticationException;

public class JwtHandler extends AuthenticationException {
    public JwtHandler(String message) {
        super(message);
    }
}
