package mju.capstone.ddingconnect.global.response.code.status;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mju.capstone.ddingconnect.global.response.code.BaseCode;
import mju.capstone.ddingconnect.global.response.code.ReasonDTO;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SuccessStatus implements BaseCode {

    //성공상태 ENUM 값
    _OK(HttpStatus.OK, "COMMON200", "성공입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    //성공 응답 생성.
    public ReasonDTO getReason() {
        return ReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(true)
                .build();
    }

    @Override
    //http상태를 담은 성공 응답 생성
    public ReasonDTO getReasonHttpStatus() {
        return ReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(true)
                .httpStatus(httpStatus)
                .build();
    }
}
