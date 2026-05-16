package mju.capstone.ddingconnect.global.response.code.status;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mju.capstone.ddingconnect.global.response.code.BaseErrorCode;
import mju.capstone.ddingconnect.global.response.code.ErrorReasonDTO;
import org.springframework.http.HttpStatus;


@Getter
@AllArgsConstructor
public enum ErrorStatus implements BaseErrorCode {

    //가장 일반적인 응답
    _INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON", "서버 에러, 관리자에게 문의 바랍니다."),
    _BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON", "잘못된 요청입니다."),
    _UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON", "인증이 필요합니다."),
    _FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON", "금지된 요청입니다."),

    // Mail
    MAIL_SEND_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "MAIL", "메일 전송에 실패했습니다."),
    MAIL_VERIFY_FAIL(HttpStatus.BAD_REQUEST, "MAIL", "인증 코드가 없거나, 일치하지 않습니다."),

    // Auth
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "AUTH", "이미 사용 중인 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH", "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH", "만료된 토큰입니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH", "존재하지 않는 회원입니다."),
    INVALID_ROLE(HttpStatus.BAD_REQUEST, "AUTH", "유효하지 않은 역할입니다. STUDENT 또는 GRADUATE만 가능합니다."),

    // Member
    MEMBER_UNAUTHORIZED(HttpStatus.FORBIDDEN, "MEMBER", "회원 정보를 수정/삭제할 권한이 없습니다."),
    MEMBER_INVALID_SOCIAL_LINK(HttpStatus.BAD_REQUEST, "MEMBER", "github 또는 linkedin 링크 형식이 올바르지 않습니다."),
    MEMBER_INVALID_GRADE(HttpStatus.BAD_REQUEST, "MEMBER", "학년은 1 이상이어야 합니다."),
    MEMBER_FIELD_ROLE_MISMATCH(HttpStatus.BAD_REQUEST, "MEMBER", "본인 역할과 일치하지 않는 필드는 수정할 수 없습니다."),

    // CoffeeChat
    COFFEE_CHAT_NOT_FOUND(HttpStatus.NOT_FOUND, "COFFEECHAT", "존재하지 않는 커피챗입니다."),
    COFFEE_CHAT_UNAUTHORIZED(HttpStatus.FORBIDDEN, "COFFEECHAT", "커피챗에 대한 권한이 없습니다."),
    COFFEE_CHAT_SELF_REQUEST(HttpStatus.BAD_REQUEST, "COFFEECHAT", "자기 자신에게는 커피챗을 요청할 수 없습니다."),
    COFFEE_CHAT_ROLE_MISMATCH(HttpStatus.BAD_REQUEST, "COFFEECHAT", "커피챗은 학생과 졸업생 사이에만 가능합니다."),

    // PostContents
    POST_CONTENTS_NOT_FOUND(HttpStatus.NOT_FOUND, "POST", "존재하지 않는 구직 공고입니다."),
    POST_CONTENTS_UNAUTHORIZED(HttpStatus.FORBIDDEN, "POST", "구직 공고를 수정/삭제할 권한이 없습니다."),
    POST_CONTENTS_NOT_GRADUATE(HttpStatus.FORBIDDEN, "POST", "졸업생만 구직 공고를 등록할 수 있습니다."),

    // Question
    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "QUESTION", "존재하지 않는 질문입니다."),
    QUESTION_UNAUTHORIZED(HttpStatus.FORBIDDEN, "QUESTION", "질문을 수정/삭제할 권한이 없습니다."),
    QUESTION_SELF_LIKE(HttpStatus.BAD_REQUEST, "QUESTION", "본인이 작성한 질문에는 좋아요를 누를 수 없습니다."),

    // Answer
    ANSWER_NOT_FOUND(HttpStatus.NOT_FOUND, "ANSWER", "존재하지 않는 답변입니다."),
    ANSWER_UNAUTHORIZED(HttpStatus.FORBIDDEN, "ANSWER", "답변을 수정/삭제할 권한이 없습니다."),
    ANSWER_SELF_LIKE(HttpStatus.BAD_REQUEST, "ANSWER", "본인이 작성한 답변에는 좋아요를 누를 수 없습니다."),
    ANSWER_NOT_GRADUATE(HttpStatus.FORBIDDEN, "ANSWER", "졸업생만 답변을 등록할 수 있습니다."),

    // Roadmap
    ROADMAP_NOT_FOUND(HttpStatus.NOT_FOUND, "ROADMAP", "존재하지 않는 로드맵입니다."),
    ROADMAP_UNAUTHORIZED(HttpStatus.FORBIDDEN, "ROADMAP", "로드맵을 수정/삭제할 권한이 없습니다."),
    ROADMAP_INVALID_CONTENT(HttpStatus.BAD_REQUEST, "ROADMAP", "로드맵 content는 JSON object 또는 array 형식이어야 합니다."),

    // TechStack
    TECH_STACK_NOT_FOUND(HttpStatus.NOT_FOUND, "TECHSTACK", "존재하지 않는 기술 스택입니다."),
    TECH_STACK_UNAUTHORIZED(HttpStatus.FORBIDDEN, "TECHSTACK", "기술 스택을 삭제할 권한이 없습니다."),
    TECH_STACK_DUPLICATE(HttpStatus.CONFLICT, "TECHSTACK", "이미 등록된 기술 스택입니다."),

    // TargetJob
    TARGET_JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "TARGETJOB", "존재하지 않는 관심 직군입니다."),
    TARGET_JOB_UNAUTHORIZED(HttpStatus.FORBIDDEN, "TARGETJOB", "관심 직군을 수정/삭제할 권한이 없습니다."),
    TARGET_JOB_DUPLICATE(HttpStatus.CONFLICT, "TARGETJOB", "이미 등록된 관심 직군입니다."),

    // Alarm
    ALARM_NOT_FOUND(HttpStatus.NOT_FOUND, "ALARM", "존재하지 않는 알람입니다."),
    ALARM_UNAUTHORIZED(HttpStatus.FORBIDDEN, "ALARM", "알람을 조회/수정할 권한이 없습니다.");

    private final HttpStatus httpStatus;
    private final String codePrefix;
    private final String message;

    /**
     * 응답 코드 = 도메인 접두사 + HTTP 상태 숫자 (예: "AUTH" + 400 → "AUTH400").
     * 숫자 접미사를 httpStatus 에서 파생해 코드/상태 불일치(드리프트)를 원천 차단한다.
     */
    public String getCode() {
        return codePrefix + httpStatus.value();
    }

    //실패 응답 생성.
    public ErrorReasonDTO getReason() {
        return ErrorReasonDTO.builder()
                .message(message)
                .code(getCode())
                .isSuccess(true)
                .build();
    }

    //http상태를 담은 실패 응답 생성
    public ErrorReasonDTO getReasonHttpStatus() {
        return ErrorReasonDTO.builder()
                .message(message)
                .code(getCode())
                .isSuccess(true)
                .httpStatus(httpStatus)
                .build();
    }
}
