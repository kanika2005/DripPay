package com.drippay.dto.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateGroupRequest(
        @NotBlank @Size(max = 120) String name,
        List<Long> memberUserIds
) {
    public CreateGroupRequest {
        if (memberUserIds == null) memberUserIds = List.of();
    }
}
