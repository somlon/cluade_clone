package mju.capstone.ddingconnect.global.sse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.global.alarm.AlarmType;
import mju.capstone.ddingconnect.global.auth.annotation.LoginMember;
import mju.capstone.ddingconnect.global.response.exception.handler.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "SSE 알람 API")
@RestController
@RequestMapping("/api/v1/notifications/test")
@RequiredArgsConstructor
public class SseTestController {

    private static final String DEFAULT_TEST_CONTENT = "테스트 알람입니다.";

    private final SseService sseService;

    @Operation(
            summary = "[테스트용] SSE 알람 발송",
            description = "현재 로그인한 사용자에게 테스트 알람을 전송합니다. SSE 구독 후 호출하세요."
    )
    @PostMapping
    public ApiResponse<String> sendTestAlarm(
            @Parameter(hidden = true) @LoginMember Member member,
            @RequestParam(required = false, defaultValue = DEFAULT_TEST_CONTENT) String content,
            @RequestParam(required = false, defaultValue = "ANSWER") AlarmType type
    ) {
        sseService.send(member, type, content);
        return ApiResponse.onSuccess("전송 완료");
    }
}
