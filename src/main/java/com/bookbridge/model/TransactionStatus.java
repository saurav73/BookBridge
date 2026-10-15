package com.bookbridge.model;

/**
 * Enumeration of possible states of a transaction.
 */
public enum TransactionStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    COMPLETED,
    CANCELLED
}
