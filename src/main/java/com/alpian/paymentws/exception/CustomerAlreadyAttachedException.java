package com.alpian.paymentws.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exception thrown when attempting to attach a PSP reference to a customer
 * that already has one.
 * Returns HTTP 409 Conflict.
 */
public class CustomerAlreadyAttachedException extends ResponseStatusException {

    private static final long serialVersionUID = 5704413520117431894L;

	public CustomerAlreadyAttachedException(String reason) {
        super(HttpStatus.CONFLICT, reason);
    }
}
