package com.alpian.paymentws.service.psp;

import com.alpian.paymentws.exception.PaymentProviderMissingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Factory that returns the appropriate ProviderPaymentService implementation
 * based on the configured payment service provider.
 */
@Component
public class PaymentServiceFactory {

    private static final String STRIPE_PROVIDER = "Stripe";

    private final String paymentProvider;
    private final StripePaymentService stripePaymentService;

    public PaymentServiceFactory(
            @Value("${payment.provider:}") String paymentProvider,
            StripePaymentService stripePaymentService) {
        this.paymentProvider = paymentProvider;
        this.stripePaymentService = stripePaymentService;
    }

    /**
     * Returns the ProviderPaymentService implementation for the configured provider.
     *
     * @return the ProviderPaymentService implementation
     * @throws PaymentProviderMissingException if the configured provider is not supported
     */
    public ProviderPaymentService getPaymentProviderService() {
        if (STRIPE_PROVIDER.equalsIgnoreCase(paymentProvider)) {
            return stripePaymentService;
        }
        throw new PaymentProviderMissingException("Unsupported payment service provider: " + paymentProvider);
    }
}
