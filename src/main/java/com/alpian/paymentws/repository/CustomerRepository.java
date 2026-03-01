package com.alpian.paymentws.repository;

import com.alpian.paymentws.domain.Customer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for Customer entities.
 */
public interface CustomerRepository extends JpaRepository<Customer, String> {

    /**
     * Find all customers matching the given first and last name.
     */
    List<Customer> findByFirstNameAndLastName(String firstName, String lastName);
}
