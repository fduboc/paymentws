package com.alpian.paymentws.service.psp;

import java.math.BigDecimal;

/**
 * Payment service provider interface for payment transactions.
 */
public interface ProviderPaymentService {

    /**
     * Create a customer at the payment service provider.
     *
     * @param customerId  internal customer id (for reference)
     * @param firstName   customer first name
     * @param lastName    customer last name
     * @param email       customer email
     * @param phoneNumber customer phone number
     * @return the PSP-generated customer id
     */
    String createCustomer(String customerId, String firstName, String lastName, String email, String phoneNumber);

    /**
     * Create a card payment intent at the PSP for the given customer.
     *
     * @param customerPspReference the customer's PSP reference (e.g. Stripe customer id)
     * @param amount                payment amount
     * @param currency              currency code (e.g. chf)
     * @param immediateCapture      should capture immediately (e.g. true)
     * @return result containing PSP payment intent id and amount capturable or received
     */
    CreatePaymentIntentResult createCardPaymentIntent(String customerPspReference, BigDecimal amount, String currency, boolean immediateCapture);
    
    /**
     * Create a SEPA payment intent at the PSP for the given customer.
     *
     * @param customerPspReference the customer's PSP reference (e.g. Stripe customer id)
     * @param amount                payment amount
     * @param currency              currency code (e.g. chf)
     * @param firstName   			customer first name
     * @param lastName    			customer last name
     * @param email       			customer email
     * @param phoneNumber 			customer phone number
     * @return result containing PSP payment intent id and amount transferred
     */
    CreatePaymentIntentResult createSEPAPaymentIntent(String customerPspReference, BigDecimal amount, String currency, String firstName, String lastName, String email, String phoneNumber);

}
