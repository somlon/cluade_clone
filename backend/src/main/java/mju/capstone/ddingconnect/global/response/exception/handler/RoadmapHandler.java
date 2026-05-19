package mju.capstone.ddingconnect.global.response.exception.handler;

import mju.capstone.ddingconnect.global.response.code.BaseErrorCode;
import mju.capstone.ddingconnect.global.response.exception.GeneralException;

public class RoadmapHandler extends GeneralException {
    public RoadmapHandler(BaseErrorCode code) {
        super(code);
    }
}
