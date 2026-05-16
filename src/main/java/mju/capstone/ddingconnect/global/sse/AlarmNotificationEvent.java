package mju.capstone.ddingconnect.global.sse;

import mju.capstone.ddingconnect.domain.member.domain.Member;

/** 알람 row 저장 직후 발행되는 이벤트. AFTER_COMMIT 시점에 AlarmNotificationListener 가 SSE 로 푸시한다. */
public record AlarmNotificationEvent(Member receiver, AlarmType type, String content) {
}
