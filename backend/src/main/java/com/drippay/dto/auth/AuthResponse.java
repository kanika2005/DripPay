package com.drippay.dto.auth;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        UserProfileResponse user
) {
    public static AuthResponse of(String token, String refreshToken, UserProfileResponse user) {
        return new AuthResponse(token, refreshToken, "Bearer", user);
    }
}
