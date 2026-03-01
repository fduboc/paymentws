package com.alpian.paymentws.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exception thrown when no supported payment service provider is configured.
 * Returns HTTP 500 Internal Server Error.
 */
public class PaymentProviderMissingException extends ResponseStatusException {

    private static final long serialVersionUID = -8247450468573154218L;

	public PaymentProviderMissingException(String reason) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, reason);
    }
}
