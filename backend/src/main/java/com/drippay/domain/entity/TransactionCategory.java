package com.drippay.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "transaction_categories")
public class TransactionCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 80)
    private String label;

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getLabel() { return label; }
}
