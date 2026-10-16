package com.drippay.dto.group;

import java.time.Instant;

public record GroupResponse(Long id, String name, Instant createdAt) {
}
