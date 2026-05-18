package mju.capstone.ddingconnect.global.common;

/**
 * [검증용 정규식·메시지 공용 상수]
 * Bean Validation @Pattern 에 쓰이는 정규식과 위반 메시지를 한 곳에 정의한다.
 * 여러 DTO 가 같은 규칙을 인라인으로 복붙하지 않도록 단일 출처를 제공한다.
 */
public final class ValidationPattern {

    /** 명지대학교 이메일(@mju.ac.kr) 형식 */
    public static final String MJU_EMAIL_REGEX = "^[a-zA-Z0-9._%+\\-]+@mju\\.ac\\.kr$";
    public static final String MJU_EMAIL_MESSAGE = "명지대학교 이메일(@mju.ac.kr)만 사용 가능합니다.";

    private ValidationPattern() {
    }
}
