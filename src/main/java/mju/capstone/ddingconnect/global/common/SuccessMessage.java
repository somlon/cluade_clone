package mju.capstone.ddingconnect.global.common;

/**
 * 컨트롤러가 ApiResponse 의 result 로 반환하는 사용자 노출 성공 메시지 상수.
 * 컨트롤러와 대응 *ControllerTest 가 동일 심볼을 참조해 메시지 중복을 제거한다.
 */
public final class SuccessMessage {

    private SuccessMessage() {}

    public static final String SIGNUP_SUCCESS = "회원가입을 성공적으로 완료하였습니다";
    public static final String MEMBER_WITHDRAWN = "회원 탈퇴가 완료되었습니다.";
    public static final String COFFEE_CHAT_CANCELED = "커피챗이 취소되었습니다.";
    public static final String JOB_POST_DELETED = "구직 공고가 삭제되었습니다.";
    public static final String TARGET_JOB_DELETED = "관심 직군이 삭제되었습니다.";
    public static final String ANSWER_DELETED = "답변이 삭제되었습니다.";
    public static final String QUESTION_DELETED = "질문이 삭제되었습니다.";
    public static final String ROADMAP_DELETED = "로드맵이 삭제되었습니다.";
    public static final String TECH_STACK_DELETED = "기술 스택이 삭제되었습니다.";
    public static final String ALARM_MARKED_AS_READ = "알람이 읽음 처리되었습니다.";
}
