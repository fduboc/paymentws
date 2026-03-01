package com.alpian.paymentws.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exception thrown when available customer account amount is insufficient
 * Returns HTTP 409 Conflict.
 */
public class InsufficientBalanceException extends ResponseStatusException {

    private static final long serialVersionUID = -6886538549939648676L;

	public InsufficientBalanceException(String reason) {
        super(HttpStatus.CONFLICT, reason);
    }
}