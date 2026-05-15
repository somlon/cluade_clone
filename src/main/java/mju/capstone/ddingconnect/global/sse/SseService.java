package mju.capstone.ddingconnect.global.sse;

import mju.capstone.ddingconnect.domain.member.domain.Member;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SseService {
    SseEmitter subscribe(Member member);
    void send(Member receiver, AlarmType type, String content);
}
