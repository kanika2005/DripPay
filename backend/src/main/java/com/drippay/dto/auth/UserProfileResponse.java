package com.drippay.dto.auth;

import com.drippay.domain.enums.UserRole;

public record UserProfileResponse(
        Long id,
        String email,
        String fullName,
        UserRole role,
        String publicWalletId,
        String dripHandle,
        boolean hasTransferPin
) {
}
