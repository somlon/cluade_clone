package mju.capstone.ddingconnect.global.response.exception.handler;

import mju.capstone.ddingconnect.global.response.code.BaseErrorCode;
import mju.capstone.ddingconnect.global.response.exception.GeneralException;

public class TechStackHandler extends GeneralException {
    public TechStackHandler(BaseErrorCode code) {
        super(code);
    }
}
