package com.alpian.paymentws.repository;

import com.alpian.paymentws.domain.PaymentIntent;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for PaymentIntent entities.
 */
public interface PaymentIntentRepository extends JpaRepository<PaymentIntent, String> {
}
