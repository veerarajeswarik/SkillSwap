package com.example.backend.entity;

/**
 * Direction of a credit transaction, from the point of view of the member.
 *
 * CREDIT -> credits were ADDED to the member's balance (the provider earned them)
 * DEBIT  -> credits were REMOVED from the member's balance (the requester spent them)
 */
public enum LedgerType {
    CREDIT,
    DEBIT
}
