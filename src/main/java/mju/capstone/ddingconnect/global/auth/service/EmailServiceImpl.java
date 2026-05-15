package mju.capstone.ddingconnect.global.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mju.capstone.ddingconnect.global.auth.dto.request.CodeSendRequest;
import mju.capstone.ddingconnect.global.auth.dto.request.VerifyCodeRequest;
import mju.capstone.ddingconnect.global.redis.RedisUtil;
import mju.capstone.ddingconnect.global.response.code.status.ErrorStatus;
import mju.capstone.ddingconnect.global.response.exception.handler.MailHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender mailSender;
    private final RedisUtil redisUtil;

    @Value("${spring.mail.username}")
    private String sender;

    private String generateRandomCode() {
        return UUID.randomUUID().toString().replaceAll("-", "").substring(0, 6).toUpperCase();
    }

    @Override
    public void sendCode(CodeSendRequest request) {
        String code = generateRandomCode();

        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(sender);
            helper.setTo(request.email());
            helper.setSubject("[띵커넥트] 이메일 인증 코드");
            helper.setText("인증 코드: " + code, false);

            //redis에 이메일과 코드 저장 (5분 간 유지)
            redisUtil.setDataWithExpire(request.email(),code,300);
            mailSender.send(message);

        } catch (MessagingException e) {
            log.error("메일 발송 실패: {}", e.getMessage());
            throw new MailHandler(ErrorStatus.MAIL_SEND_FAIL);
        }

    }

    @Override
    public void verifyCode(VerifyCodeRequest request) {
        String data = redisUtil.getData(request.email());
        // 존재하지 않거나 일치하지 않으면 에러
        if (!request.code().equals(data)) {
            throw new MailHandler(ErrorStatus.MAIL_VERIFY_FAIL);
        }
        // 비우기
        redisUtil.deleteData(request.email());
    }

}
