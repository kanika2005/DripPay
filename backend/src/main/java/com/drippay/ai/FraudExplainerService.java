package com.drippay.ai;

import com.drippay.domain.entity.SecurityAlert;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FraudExplainerService {

    private final String llmApiKey;

    public FraudExplainerService(@Value("${drippay.ai.api-key:}") String llmApiKey) {
        this.llmApiKey = llmApiKey == null ? "" : llmApiKey.trim();
    }

    /**
     * Rules engine already flagged the alert — this layer only explains in plain English.
     */
    public String explainForAdmin(SecurityAlert alert) {
        String base = "Potentially suspicious activity (not confirmed fraud). "
                + "Rule context: " + alert.getReason()
                + " Suggested action: review recent transfers, contact the user if needed, and mark the alert reviewed.";
        if (!llmApiKey.isEmpty()) {
            // Optional LLM polish could be added here; template keeps tests deterministic without network.
            return base + " [LLM key configured — template mode for v1]";
        }
        return base;
    }
}
