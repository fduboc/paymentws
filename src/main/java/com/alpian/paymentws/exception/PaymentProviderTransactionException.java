package com.alpian.paymentws.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exception thrown when executing payment service provider transaction.
 * Returns HTTP 500 Internal Server Error.
 */
public class PaymentProviderTransactionException extends ResponseStatusException {

    private static final long serialVersionUID = 3217870256557988064L;

	public PaymentProviderTransactionException(String reason) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, reason);
    }
    
    public PaymentProviderTransactionException(String reason, Exception e) {
		super(HttpStatus.INTERNAL_SERVER_ERROR, reason, e);
	}
}