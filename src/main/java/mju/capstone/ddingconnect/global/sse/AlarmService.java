package mju.capstone.ddingconnect.global.sse;

import mju.capstone.ddingconnect.domain.member.domain.Member;

import java.util.List;

/**
 * [통합 알람 서비스 인터페이스]
 * 4종 알람을 한 번에 조회/관리하기 위한 공통 인터페이스
 */
public interface AlarmService {

    /** 내 알람 전체 목록 조회 (시간 내림차순) */
    List<AlarmResponse> getMyAlarms(Member member);

    /** 알람 상세 조회 (타입 + PK 기반) */
    AlarmResponse getAlarmDetail(Member member, AlarmType type, Long alarmId);

    /** 알람 읽음 처리 */
    void markAsRead(Member member, AlarmType type, Long alarmId);
}
