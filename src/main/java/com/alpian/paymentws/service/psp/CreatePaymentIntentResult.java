package com.alpian.paymentws.service.psp;

import java.math.BigDecimal;

/**
 * Result of creating a payment intent at the PSP.
 */
public class CreatePaymentIntentResult {

    private final String pspPaymentIntentId;
    private final BigDecimal amount;

    public CreatePaymentIntentResult(String pspPaymentIntentId, BigDecimal amount) {
        this.pspPaymentIntentId = pspPaymentIntentId;
        this.amount = amount;
    }

    public String getPspPaymentIntentId() {
        return pspPaymentIntentId;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
