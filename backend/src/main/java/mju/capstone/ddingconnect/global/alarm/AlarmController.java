package mju.capstone.ddingconnect.global.alarm;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.global.auth.annotation.LoginMember;
import mju.capstone.ddingconnect.global.common.SuccessMessage;
import mju.capstone.ddingconnect.global.response.exception.handler.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * [통합 알람 컨트롤러]
 * 4종 알람(답변/구직/로드맵/커피챗)을 하나의 엔드포인트에서 통합 제공
 */
@RestController
@RequestMapping("/api/v1/alarms")
@RequiredArgsConstructor
public class AlarmController implements AlarmSwagger {

    private final AlarmService alarmService;

    /** 내 알람 전체 목록 조회 (시간 내림차순) */
    @GetMapping
    public ApiResponse<List<AlarmResponse>> getMyAlarms(
            @Parameter(hidden = true) @LoginMember Member member) {
        return ApiResponse.onSuccess(alarmService.getMyAlarms(member));
    }

    /** 알람 상세 조회 */
    @GetMapping("/{type}/{alarmId}")
    public ApiResponse<AlarmResponse> getAlarmDetail(
            @Parameter(hidden = true) @LoginMember Member member,
            @PathVariable AlarmType type,
            @PathVariable Long alarmId) {
        return ApiResponse.onSuccess(alarmService.getAlarmDetail(member, type, alarmId));
    }

    /** 알람 읽음 처리 */
    @PatchMapping("/{type}/{alarmId}/read")
    public ApiResponse<String> markAsRead(
            @Parameter(hidden = true) @LoginMember Member member,
            @PathVariable AlarmType type,
            @PathVariable Long alarmId) {
        alarmService.markAsRead(member, type, alarmId);
        return ApiResponse.onSuccess(SuccessMessage.ALARM_MARKED_AS_READ);
    }
}
