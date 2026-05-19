package mju.capstone.ddingconnect.global.alarm;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.global.auth.annotation.LoginMember;
import mju.capstone.ddingconnect.global.response.exception.handler.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "알람", description = "통합 알람 컨트롤러 — 답변/구직/로드맵/커피챗 4종 알람의 목록 조회/상세 조회/읽음 처리 엔드포인트를 제공합니다.")
public interface AlarmSwagger {

    @Operation(
            summary = "내 알람 전체 목록 조회",
            description = "로그인된 회원의 모든 알람을 시간 내림차순으로 조회합니다."
    )
    @GetMapping
    ApiResponse<List<AlarmResponse>> getMyAlarms(
            @Parameter(hidden = true) @LoginMember Member member);




    @Operation(
            summary = "알람 상세 조회",
            description = "알람 타입(ANSWER/JOB/ROADMAP/COFFEE_CHAT)과 알람 ID로 특정 알람의 상세 정보를 조회합니다."
    )
    @GetMapping("/{type}/{alarmId}")
    ApiResponse<AlarmResponse> getAlarmDetail(
            @Parameter(hidden = true) @LoginMember Member member,
            @Parameter(description = "알람 타입 (ANSWER, JOB, ROADMAP, COFFEE_CHAT)")
            @PathVariable AlarmType type,
            @Parameter(description = "조회할 알람 ID")
            @PathVariable Long alarmId);




    @Operation(
            summary = "알람 읽음 처리",
            description = "특정 알람을 읽음 상태로 변경합니다."
    )
    @PatchMapping("/{type}/{alarmId}/read")
    ApiResponse<String> markAsRead(
            @Parameter(hidden = true) @LoginMember Member member,
            @Parameter(description = "알람 타입 (ANSWER, JOB, ROADMAP, COFFEE_CHAT)")
            @PathVariable AlarmType type,
            @Parameter(description = "읽음 처리할 알람 ID")
            @PathVariable Long alarmId);

}
