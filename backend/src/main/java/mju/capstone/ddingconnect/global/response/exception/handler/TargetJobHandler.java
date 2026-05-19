package mju.capstone.ddingconnect.global.response.exception.handler;

import mju.capstone.ddingconnect.global.response.code.BaseErrorCode;
import mju.capstone.ddingconnect.global.response.exception.GeneralException;

public class TargetJobHandler extends GeneralException {

    public TargetJobHandler(BaseErrorCode code) {
        super(code);
    }
}
