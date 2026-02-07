package com.example.community.member.exception;

import com.example.community.global.exception.GeneralException;
import com.example.community.global.response.code.status.ErrorStatus;

public class SamePasswordException extends GeneralException {
    public SamePasswordException() {
        super(ErrorStatus.SAME_PASSWORD);
    }
}


