package mju.capstone.ddingconnect.global.response.exception.handler;

import mju.capstone.ddingconnect.global.response.code.BaseErrorCode;
import mju.capstone.ddingconnect.global.response.exception.GeneralException;

public class MailHandler extends GeneralException {
    public MailHandler(BaseErrorCode code) {
        super(code);
    }
}
