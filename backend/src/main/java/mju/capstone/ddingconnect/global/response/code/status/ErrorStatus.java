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
    MEMBER_INVALID_SOCIAL_LINK(HttpStatus.BAD_REQUEST, "MEMBER400", "github 또는 linkedin 링크 형식이 올바르지 않습니다."),
    MEMBER_INVALID_GRADE(HttpStatus.BAD_REQUEST, "MEMBER400", "학년은 1 이상이어야 합니다."),
    MEMBER_FIELD_ROLE_MISMATCH(HttpStatus.BAD_REQUEST, "MEMBER400", "본인 역할과 일치하지 않는 필드는 수정할 수 없습니다."),

    // CoffeeChat
    COFFEE_CHAT_NOT_FOUND(HttpStatus.NOT_FOUND, "COFFEECHAT404", "존재하지 않는 커피챗입니다."),
    COFFEE_CHAT_UNAUTHORIZED(HttpStatus.FORBIDDEN, "COFFEECHAT403", "커피챗에 대한 권한이 없습니다."),
    COFFEE_CHAT_SELF_REQUEST(HttpStatus.BAD_REQUEST, "COFFEECHAT400", "자기 자신에게는 커피챗을 요청할 수 없습니다."),
    COFFEE_CHAT_ROLE_MISMATCH(HttpStatus.BAD_REQUEST, "COFFEECHAT400", "커피챗은 학생과 졸업생 사이에만 가능합니다."),
    COFFEE_CHAT_ALREADY_REQUESTED(HttpStatus.BAD_REQUEST, "COFFEECHAT400", "이미 진행 중인 커피챗 요청이 있습니다."),
    COFFEE_CHAT_REQUEST_TOO_SOON(HttpStatus.TOO_MANY_REQUESTS, "COFFEECHAT429", "커피챗 재요청은 일정 시간이 지난 후에 가능합니다."),
    MATCHING_ALGORITHM_FAILED(HttpStatus.BAD_GATEWAY, "COFFEECHAT502", "매칭 알고리즘 호출에 실패했습니다."),

    // PostContents
    POST_CONTENTS_NOT_FOUND(HttpStatus.NOT_FOUND, "POST404", "존재하지 않는 구직 공고입니다."),
    POST_CONTENTS_UNAUTHORIZED(HttpStatus.FORBIDDEN, "POST403", "구직 공고를 수정/삭제할 권한이 없습니다."),
    POST_CONTENTS_NOT_GRADUATE(HttpStatus.FORBIDDEN, "POST403", "졸업생만 구직 공고를 등록할 수 있습니다."),

    // Question
    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "QUESTION404", "존재하지 않는 질문입니다."),
    QUESTION_UNAUTHORIZED(HttpStatus.FORBIDDEN, "QUESTION403", "질문을 수정/삭제할 권한이 없습니다."),
    QUESTION_SELF_LIKE(HttpStatus.BAD_REQUEST, "QUESTION400", "본인이 작성한 질문에는 좋아요를 누를 수 없습니다."),

    // Answer
    ANSWER_NOT_FOUND(HttpStatus.NOT_FOUND, "ANSWER404", "존재하지 않는 답변입니다."),
    ANSWER_UNAUTHORIZED(HttpStatus.FORBIDDEN, "ANSWER403", "답변을 수정/삭제할 권한이 없습니다."),
    ANSWER_SELF_LIKE(HttpStatus.BAD_REQUEST, "ANSWER400", "본인이 작성한 답변에는 좋아요를 누를 수 없습니다."),
    ANSWER_NOT_GRADUATE(HttpStatus.FORBIDDEN, "ANSWER403", "졸업생만 답변을 등록할 수 있습니다."),

    // Roadmap
    ROADMAP_NOT_FOUND(HttpStatus.NOT_FOUND, "ROADMAP404", "존재하지 않는 로드맵입니다."),
    ROADMAP_UNAUTHORIZED(HttpStatus.FORBIDDEN, "ROADMAP403", "로드맵을 수정/삭제할 권한이 없습니다."),
    ROADMAP_INVALID_CONTENT(HttpStatus.BAD_REQUEST, "ROADMAP400", "로드맵 content는 비어 있을 수 없습니다."),
    ROADMAP_AI_GENERATION_FAILED(HttpStatus.BAD_GATEWAY, "ROADMAP502", "로드맵 AI 생성에 실패했습니다."),

    // Alarm
    ALARM_NOT_FOUND(HttpStatus.NOT_FOUND, "ALARM404", "존재하지 않는 알람입니다."),
    ALARM_UNAUTHORIZED(HttpStatus.FORBIDDEN, "ALARM403", "알람을 조회/수정할 권한이 없습니다."),

    //S3
    _FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S3500", "객체 저장에 실패하였습니다."),
    _FILE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S3500", "객체 삭제에 실패하였습니다."),
    _FILE_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "S3400", "허용되지 않은 파일 형식입니다."),
    _FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "S3400", "파일 크기가 허용된 최대치를 초과합니다.");
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
