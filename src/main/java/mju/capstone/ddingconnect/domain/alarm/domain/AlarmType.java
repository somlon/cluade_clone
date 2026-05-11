package mju.capstone.ddingconnect.domain.alarm.domain;

/**
 * [알람 종류]
 * 4종 알람을 통합 응답으로 묶기 위한 식별자
 * - ANSWER       : 답변 알람 (질문 작성자에게 발송)
 * - JOB          : 구직 알람 (회원에게 직접 발송)
 * - ROADMAP      : 로드맵 알람 (로드맵 작성자에게 발송)
 * - COFFEE_CHAT  : 커피챗 알람 (요청자/수신자에게 발송)
 */
public enum AlarmType {
    ANSWER,
    JOB,
    ROADMAP,
    COFFEE_CHAT
}
