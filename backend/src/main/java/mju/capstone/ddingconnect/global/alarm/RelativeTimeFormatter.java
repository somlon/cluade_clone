package mju.capstone.ddingconnect.global.alarm;

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

    private static final long SECONDS_PER_MINUTE = 60L;
    private static final long MINUTES_PER_HOUR = 60L;
    private static final long HOURS_PER_DAY = 24L;
    private static final long DAYS_PER_MONTH = 30L;  // 1개월 ≈ 30일 근사
    private static final long DAYS_PER_YEAR = 365L;  // 1년 ≈ 365일 근사
    private static final long MIN_ELAPSED_SECONDS = 0L;        // 미래 시각 방어 하한
    private static final long JUST_NOW_THRESHOLD_MINUTES = 1L; // 미만이면 "방금 전"

    private RelativeTimeFormatter() {}

    public static String format(LocalDateTime createdAt) {
        return format(createdAt, LocalDateTime.now());
    }

    static String format(LocalDateTime createdAt, LocalDateTime now) {
        if (createdAt == null) return "";

        long seconds = Duration.between(createdAt, now).getSeconds();
        if (seconds < MIN_ELAPSED_SECONDS) seconds = MIN_ELAPSED_SECONDS; // 미래 시각 방어

        long minutes = seconds / SECONDS_PER_MINUTE;
        if (minutes < JUST_NOW_THRESHOLD_MINUTES) return "방금 전";
        if (minutes < MINUTES_PER_HOUR) return minutes + "분 전";

        long hours = minutes / MINUTES_PER_HOUR;
        if (hours < HOURS_PER_DAY) return hours + "시간 전";

        long days = hours / HOURS_PER_DAY;
        if (days < DAYS_PER_MONTH) return days + "일 전";
        if (days < DAYS_PER_YEAR) return (days / DAYS_PER_MONTH) + "개월 전";

        return (days / DAYS_PER_YEAR) + "년 전";
    }
}
