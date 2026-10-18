package com.drippay.risk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class RiskRuleConfig {

    private RiskRuleConfig() {
    }

    public static int intValue(JsonNode root, String field, int defaultValue) {
        if (root == null || !root.has(field)) {
            return defaultValue;
        }
        return root.get(field).asInt(defaultValue);
    }

    public static JsonNode parse(String configJson, ObjectMapper mapper) {
        if (configJson == null || configJson.isBlank()) {
            return mapper.createObjectNode();
        }
        try {
            return mapper.readTree(configJson);
        } catch (Exception e) {
            return mapper.createObjectNode();
        }
    }
}
