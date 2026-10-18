package com.drippay.dto.insights;

public record ChatResponse(
        String reply,
        Long paymentDraftId,
        String draftSummary
) {
    public static ChatResponse text(String reply) {
        return new ChatResponse(reply, null, null);
    }

    public static ChatResponse withDraft(String reply, Long draftId, String summary) {
        return new ChatResponse(reply, draftId, summary);
    }
}
