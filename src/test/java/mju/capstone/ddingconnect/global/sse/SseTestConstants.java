package mju.capstone.ddingconnect.global.sse;

public final class SseTestConstants {

    private SseTestConstants() {}

    public static final Long TEST_MEMBER_ID = 1L;
    public static final String TEST_EMAIL = "test@mju.ac.kr";
    public static final String TEST_NICKNAME = "테스터";
    public static final String TEST_PASSWORD = "encoded_password";

    public static final String SUBSCRIBE_URL = "/api/v1/notifications/subscribe";
    public static final String TEST_ALARM_CONTENT = "테스트 알람 내용입니다.";
    public static final long HEARTBEAT_INTERVAL_SECONDS = 10L;
}
