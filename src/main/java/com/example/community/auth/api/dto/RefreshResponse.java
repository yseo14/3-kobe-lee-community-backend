package com.example.community.auth.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RefreshResponse(
        String accessToken,

        //  컨트롤러에서 읽을 수 있지만, 직렬화에서 제외함으로써 브라우저 body에 노출시키지 않는다.
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        String refreshToken
) {
}