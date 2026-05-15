package mju.capstone.ddingconnect.global.sse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RelativeTimeFormatter 단위 테스트")
class RelativeTimeFormatterTest {

    private final LocalDateTime now = LocalDateTime.of(2026, 5, 11, 12, 0, 0);

    @Test
    @DisplayName("null → 빈 문자열")
    void nullReturnsEmpty() {
        assertThat(RelativeTimeFormatter.format(null, now)).isEqualTo("");
    }

    @Test
    @DisplayName("< 1분 → '방금 전'")
    void underOneMinute() {
        assertThat(RelativeTimeFormatter.format(now.minusSeconds(0), now)).isEqualTo("방금 전");
        assertThat(RelativeTimeFormatter.format(now.minusSeconds(30), now)).isEqualTo("방금 전");
        assertThat(RelativeTimeFormatter.format(now.minusSeconds(59), now)).isEqualTo("방금 전");
    }

    @Test
    @DisplayName("1분 ~ 59분 → 'N분 전'")
    void underOneHour() {
        assertThat(RelativeTimeFormatter.format(now.minusMinutes(1), now)).isEqualTo("1분 전");
        assertThat(RelativeTimeFormatter.format(now.minusMinutes(30), now)).isEqualTo("30분 전");
        assertThat(RelativeTimeFormatter.format(now.minusMinutes(59), now)).isEqualTo("59분 전");
    }

    @Test
    @DisplayName("1시간 ~ 23시간 → 'N시간 전'")
    void underOneDay() {
        assertThat(RelativeTimeFormatter.format(now.minusHours(1), now)).isEqualTo("1시간 전");
        assertThat(RelativeTimeFormatter.format(now.minusHours(5), now)).isEqualTo("5시간 전");
        assertThat(RelativeTimeFormatter.format(now.minusHours(23), now)).isEqualTo("23시간 전");
    }

    @Test
    @DisplayName("1일 ~ 29일 → 'N일 전'")
    void underOneMonth() {
        assertThat(RelativeTimeFormatter.format(now.minusDays(1), now)).isEqualTo("1일 전");
        assertThat(RelativeTimeFormatter.format(now.minusDays(3), now)).isEqualTo("3일 전");
        assertThat(RelativeTimeFormatter.format(now.minusDays(29), now)).isEqualTo("29일 전");
    }

    @Test
    @DisplayName("30일 ~ 359일 → 'N개월 전'")
    void underOneYear() {
        assertThat(RelativeTimeFormatter.format(now.minusDays(30), now)).isEqualTo("1개월 전");
        assertThat(RelativeTimeFormatter.format(now.minusDays(180), now)).isEqualTo("6개월 전");
        assertThat(RelativeTimeFormatter.format(now.minusDays(359), now)).isEqualTo("11개월 전");
    }

    @Test
    @DisplayName("365일 이상 → 'N년 전'")
    void overOneYear() {
        assertThat(RelativeTimeFormatter.format(now.minusDays(365), now)).isEqualTo("1년 전");
        assertThat(RelativeTimeFormatter.format(now.minusDays(365 * 3), now)).isEqualTo("3년 전");
    }

    @Test
    @DisplayName("미래 시각 → '방금 전' (음수 방어)")
    void futureClampsToJustNow() {
        assertThat(RelativeTimeFormatter.format(now.plusMinutes(10), now)).isEqualTo("방금 전");
    }
}
