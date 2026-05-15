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
    _INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,"COMMON500", "서버 에러, 관리자에게 문의 바랍니다."),
    _BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON400", "잘못된 요청입니다."),
    _UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON401", "인증이 필요합니다."),
    _FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON403","금지된 요청입니다."),

    // Mail
    MAIL_SEND_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "MAIL500", "메일 전송에 실패했습니다."),
    MAIL_VERIFY_FAIL(HttpStatus.BAD_REQUEST, "MAIL400", "인증 코드가 없거나, 일치하지 않습니다."),

    // Auth
    DUPLICATE_EMAIL(HttpStatus.CONFLICT,"AUTH409", "이미 사용 중인 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH401", "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH401","유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH401","만료된 토큰입니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH404","존재하지 않는 회원입니다."),
    INVALID_ROLE(HttpStatus.BAD_REQUEST, "AUTH404","유효하지 않은 역할입니다. STUDENT 또는 GRADUATE만 가능합니다."),

    // Member
    MEMBER_UNAUTHORIZED(HttpStatus.FORBIDDEN, "MEMBER403", "회원 정보를 수정/삭제할 권한이 없습니다."),

    // CoffeeChat
    COFFEE_CHAT_NOT_FOUND(HttpStatus.NOT_FOUND, "COFFEECHAT404", "존재하지 않는 커피챗입니다."),
    COFFEE_CHAT_UNAUTHORIZED(HttpStatus.FORBIDDEN, "COFFEECHAT403", "커피챗에 대한 권한이 없습니다."),

    // PostContents
    POST_CONTENTS_NOT_FOUND(HttpStatus.NOT_FOUND, "POST404", "존재하지 않는 구직 공고입니다."),
    POST_CONTENTS_UNAUTHORIZED(HttpStatus.FORBIDDEN, "POST403", "구직 공고를 수정/삭제할 권한이 없습니다."),
    POST_CONTENTS_NOT_GRADUATE(HttpStatus.FORBIDDEN, "POST403", "졸업생만 구직 공고를 등록할 수 있습니다."),

    // Question
    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "QUESTION404", "존재하지 않는 질문입니다."),
    QUESTION_UNAUTHORIZED(HttpStatus.FORBIDDEN, "QUESTION403", "질문을 수정/삭제할 권한이 없습니다."),

    // Answer
    ANSWER_NOT_FOUND(HttpStatus.NOT_FOUND, "ANSWER404", "존재하지 않는 답변입니다."),
    ANSWER_UNAUTHORIZED(HttpStatus.FORBIDDEN, "ANSWER403", "답변을 수정/삭제할 권한이 없습니다."),

    // Roadmap
    ROADMAP_NOT_FOUND(HttpStatus.NOT_FOUND, "ROADMAP404", "존재하지 않는 로드맵입니다."),
    ROADMAP_UNAUTHORIZED(HttpStatus.FORBIDDEN, "ROADMAP403", "로드맵을 수정/삭제할 권한이 없습니다."),

    // TechStack
    TECH_STACK_NOT_FOUND(HttpStatus.NOT_FOUND, "TECHSTACK404", "존재하지 않는 기술 스택입니다."),
    TECH_STACK_UNAUTHORIZED(HttpStatus.FORBIDDEN, "TECHSTACK403", "기술 스택을 삭제할 권한이 없습니다."),

    // TargetJob
    TARGET_JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "TARGETJOB404", "존재하지 않는 관심 직군입니다."),
    TARGET_JOB_UNAUTHORIZED(HttpStatus.FORBIDDEN, "TARGETJOB403", "관심 직군을 수정/삭제할 권한이 없습니다."),

    // Alarm
    ALARM_NOT_FOUND(HttpStatus.NOT_FOUND, "ALARM404", "존재하지 않는 알람입니다."),
    ALARM_UNAUTHORIZED(HttpStatus.FORBIDDEN, "ALARM403", "알람을 조회/수정할 권한이 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    //실패 응답 생성.
    public ErrorReasonDTO getReason() {
        return ErrorReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(true)
                .build();
    }

    //http상태를 담은 실패 응답 생성
    public ErrorReasonDTO getReasonHttpStatus() {
        return ErrorReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(true)
                .httpStatus(httpStatus)
                .build();
    }
}
