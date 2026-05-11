package mju.capstone.ddingconnect.global.response.exception.handler;

import mju.capstone.ddingconnect.global.response.code.BaseErrorCode;
import mju.capstone.ddingconnect.global.response.exception.GeneralException;

public class JobPostHandler extends GeneralException {
    public JobPostHandler(BaseErrorCode code) {
        super(code);
    }
}
