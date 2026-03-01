package com.alpian.paymentws.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exception thrown when provided payment amount is not positive.
 * Returns HTTP 400 Bad Request.
 */
public class PaymentAmountException extends ResponseStatusException {

    private static final long serialVersionUID = -6886538549939648676L;

	public PaymentAmountException(String reason) {
        super(HttpStatus.BAD_REQUEST, reason);
    }
}
