package mju.capstone.ddingconnect.global.response.exception.handler;

import mju.capstone.ddingconnect.global.response.code.BaseErrorCode;
import mju.capstone.ddingconnect.global.response.exception.GeneralException;

public class AuthHandler extends GeneralException {
    public AuthHandler(BaseErrorCode code) {
        super(code);
    }

}
