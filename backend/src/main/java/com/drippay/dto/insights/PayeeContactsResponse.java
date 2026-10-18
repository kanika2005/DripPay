package com.drippay.dto.insights;

import java.util.List;

public record PayeeContactsResponse(
        List<PayeeContactResponse> recentlyPaid,
        List<PayeeContactResponse> mostContacted
) {
}
