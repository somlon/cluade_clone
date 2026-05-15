package mju.capstone.ddingconnect.global.sse;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * [상대 시간 포맷터]
 * 알람 응답에서 createdAt 을 "방금 전 / N분 전 / N시간 전 / N일 전 / N개월 전 / N년 전"
 * 으로 변환. 매 요청마다 재계산되어 시간 흐름에 따라 값이 자연스럽게 갱신된다.
 *
 * 임계값:
 * - < 1분        → "방금 전"
 * - < 60분       → "N분 전"
 * - < 24시간     → "N시간 전"
 * - < 30일       → "N일 전"
 * - < 12개월     → "N개월 전" (1개월 = 30일 근사)
 * - 그 이상      → "N년 전"  (1년  = 365일 근사)
 *
 * createdAt 이 null 이면 빈 문자열 반환 (방어).
 */
public final class RelativeTimeFormatter {

    private RelativeTimeFormatter() {}

    public static String format(LocalDateTime createdAt) {
        return format(createdAt, LocalDateTime.now());
    }

    static String format(LocalDateTime createdAt, LocalDateTime now) {
        if (createdAt == null) return "";

        long seconds = Duration.between(createdAt, now).getSeconds();
        if (seconds < 0) seconds = 0; // 미래 시각 방어

        long minutes = seconds / 60;
        if (minutes < 1) return "방금 전";
        if (minutes < 60) return minutes + "분 전";

        long hours = minutes / 60;
        if (hours < 24) return hours + "시간 전";

        long days = hours / 24;
        if (days < 30) return days + "일 전";
        if (days < 365) return (days / 30) + "개월 전";

        return (days / 365) + "년 전";
    }
}
