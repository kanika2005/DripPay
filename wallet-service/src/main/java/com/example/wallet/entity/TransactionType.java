package com.example.wallet.entity;

/**
 * Transaction type stored in wallet_transaction.type column.
 *
 * CREDIT = money added to wallet
 * DEBIT  = money withdrawn from wallet
 *
 * Why an enum?
 * - Prevents typos like "creditt" in the database
 * - IDE autocomplete
 * - Clear domain meaning
 */
public enum TransactionType {
    CREDIT,
    DEBIT
}
