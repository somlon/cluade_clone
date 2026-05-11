package mju.capstone.ddingconnect.global.response.exception.handler;

import mju.capstone.ddingconnect.global.response.code.BaseErrorCode;
import mju.capstone.ddingconnect.global.response.exception.GeneralException;

public class AlarmHandler extends GeneralException {
    public AlarmHandler(BaseErrorCode code) {
        super(code);
    }
}
