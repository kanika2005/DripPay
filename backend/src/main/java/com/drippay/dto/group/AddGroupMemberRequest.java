package com.drippay.dto.group;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Friend's DripPay email or public wallet ID (DRIP…). */
public record AddGroupMemberRequest(
        @NotBlank @Size(max = 120) @JsonAlias("email") String invite
) {
}
