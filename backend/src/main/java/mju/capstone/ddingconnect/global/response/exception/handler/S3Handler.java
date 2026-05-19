package mju.capstone.ddingconnect.global.response.exception.handler;

import mju.capstone.ddingconnect.global.response.code.BaseErrorCode;
import mju.capstone.ddingconnect.global.response.exception.GeneralException;

public class S3Handler extends GeneralException {
    public S3Handler(BaseErrorCode code) {
        super(code);
    }}
