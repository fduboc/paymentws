package com.alpian.paymentws.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exception thrown when a customer account already exists.
 * Returns HTTP 400.
 */
public class CustomerAccountAlreadyExistsException extends ResponseStatusException {

	private static final long serialVersionUID = -6306846802097005635L;

	public CustomerAccountAlreadyExistsException(String reason) {
        super(HttpStatus.BAD_REQUEST, reason);
    }
}