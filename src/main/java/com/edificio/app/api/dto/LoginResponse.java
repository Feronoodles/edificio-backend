package com.edificio.app.api.dto;

import java.time.Instant;

public record LoginResponse(
        String tokenType,
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt
) {
}
