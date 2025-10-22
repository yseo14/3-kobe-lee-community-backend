package com.example.community.auth.Exception;

import com.example.community.global.exception.GeneralException;
import com.example.community.global.response.code.status.ErrorStatus;

public class TokenNotFoundException extends GeneralException {
    public TokenNotFoundException() {
        super(ErrorStatus.TOKEN_NOT_FOUND);
    }
}
