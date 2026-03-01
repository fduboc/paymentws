package com.alpian.paymentws.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

import com.alpian.paymentws.domain.CustomerAccount;

/**
 * Spring Data JPA repository for Customer Account entities.
 */
public interface CustomerAccountRepository extends JpaRepository<CustomerAccount, String> {
	Optional<CustomerAccount> findByCustomerId(String customerId);
	
	@Modifying
	@Query("UPDATE CustomerAccount a SET a.availableAmount = a.availableAmount - :amount WHERE a.customerId = :customerId")
	int subtractAmount(@Param("customerId") String customerId, @Param("amount") BigDecimal amount);
}
