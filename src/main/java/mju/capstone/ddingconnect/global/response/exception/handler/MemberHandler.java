package mju.capstone.ddingconnect.global.response.exception.handler;

import mju.capstone.ddingconnect.global.response.code.BaseErrorCode;
import mju.capstone.ddingconnect.global.response.exception.GeneralException;

public class MemberHandler extends GeneralException {

    public MemberHandler(BaseErrorCode code) {
        super(code);
    }
}
