package mju.capstone.ddingconnect.global.response.exception.handler;

import mju.capstone.ddingconnect.global.response.code.BaseErrorCode;
import mju.capstone.ddingconnect.global.response.exception.GeneralException;

public class CoffeeChatHandler extends GeneralException {
    public CoffeeChatHandler(BaseErrorCode code) {
        super(code);
    }
}
