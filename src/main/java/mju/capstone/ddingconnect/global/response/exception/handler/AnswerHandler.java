package mju.capstone.ddingconnect.global.response.exception.handler;

import mju.capstone.ddingconnect.global.response.code.BaseErrorCode;
import mju.capstone.ddingconnect.global.response.exception.GeneralException;

public class AnswerHandler extends GeneralException {
    public AnswerHandler(BaseErrorCode code) {
        super(code);
    }
}
