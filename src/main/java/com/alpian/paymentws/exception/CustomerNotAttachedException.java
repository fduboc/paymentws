package com.alpian.paymentws.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exception thrown when attempting to perform a transaction for a a customer not attached with a PSP reference
 * Returns HTTP 409 Conflict.
 */
public class CustomerNotAttachedException extends ResponseStatusException {

	private static final long serialVersionUID = 3156826557385470185L;

	public CustomerNotAttachedException(String reason) {
        super(HttpStatus.CONFLICT, reason);
    }
}