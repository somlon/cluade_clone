package mju.capstone.ddingconnect.global.response.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mju.capstone.ddingconnect.global.response.code.ErrorReasonDTO;

@Getter
@AllArgsConstructor
public class GeneralException extends RuntimeException {
    private mju.capstone.ddingconnect.global.response.code.BaseErrorCode code;

    public ErrorReasonDTO getErrorReason() {
        return this.code.getReason();
    }
    public ErrorReasonDTO getErrorReasonHttpStatus() {
        return this.code.getReasonHttpStatus();
    }
}
