package com.drippay.dto.common;

import java.util.List;

/**
 * Stable JSON shape for paginated API responses (avoids serializing {@code PageImpl} directly).
 */
public record PageResponse<T>(
        List<T> content,
        int totalPages,
        long totalElements,
        int number,
        int size
) {
    public static <T> PageResponse<T> from(org.springframework.data.domain.Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.getNumber(),
                page.getSize()
        );
    }
}
