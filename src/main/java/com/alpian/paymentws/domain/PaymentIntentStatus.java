package com.alpian.paymentws.domain;

/**
 * Status of a payment intent at the PSP.
 */
public enum PaymentIntentStatus {
    CREATED,
    SUCCEEDED,
    FAILED,
    CANCELED,
    REQUIRES_ACTION,
    REQUIRES_CONFIRMATION
}
