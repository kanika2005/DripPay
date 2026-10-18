package com.drippay.risk;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "risk_rules")
public class RiskRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "config_json", columnDefinition = "JSON")
    private String configJson;

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getDescription() { return description; }
    public boolean isEnabled() { return enabled; }
    public String getConfigJson() { return configJson; }
}
