package com.drippay.dto.auth;

public record AuthResponse(
        String accessToken,
        String tokenType,
        UserProfileResponse user
) {
    public static AuthResponse of(String token, UserProfileResponse user) {
        return new AuthResponse(token, "Bearer", user);
    }
}
