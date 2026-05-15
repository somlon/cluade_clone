package mju.capstone.ddingconnect.global.sse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.global.auth.annotation.LoginMember;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "SSE 알람 API", description = "SSE를 통한 실시간 알람 API입니다.")
public interface SseSwagger {

    @Operation(
            summary = "SSE 구독 API",
            description = "SSE 연결을 맺습니다. 상단 Authorize 버튼에서 JWT 토큰을 먼저 입력하세요. 연결 후 10초마다 ping 이벤트가 수신됩니다."
    )
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter subscribe(@Parameter(hidden = true) @LoginMember Member member);
}
