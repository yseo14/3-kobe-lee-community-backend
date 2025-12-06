package com.example.community.member.exception;

import com.example.community.global.exception.GeneralException;
import com.example.community.global.response.code.status.ErrorStatus;

public class InvalidCurrentPasswordException extends GeneralException {
    public InvalidCurrentPasswordException() {
        super(ErrorStatus.INVALID_CURRENT_PASSWORD);
    }
}

