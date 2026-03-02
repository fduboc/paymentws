package com.alpian.paymentws.service;

import java.math.BigDecimal;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alpian.paymentws.domain.CustomerAccount;
import com.alpian.paymentws.dto.CustomerAccountDTO;
import com.alpian.paymentws.exception.CustomerAccountAlreadyExistsException;
import com.alpian.paymentws.exception.CustomerAccountNotFoundException;
import com.alpian.paymentws.repository.CustomerAccountRepository;

/**
 * Service layer for Customer Account CRUD and credit operations.
 * Maps between CustomerAccountDTO and CustomerAccount entity using ModelMapper.
 */
@Transactional
@Service
public class CustomerAccountService {
	private final Logger LOG = LoggerFactory.getLogger(CustomerAccountService.class);
	
	private final CustomerAccountRepository customerAccountRepository;
    private final ModelMapper modelMapper;

    public CustomerAccountService(CustomerAccountRepository customerAccountRepository, ModelMapper modelMapper) {
        this.customerAccountRepository = customerAccountRepository;
        this.modelMapper = modelMapper;
    }

    public CustomerAccountDTO createCustomerAccount(String customerId, CustomerAccountDTO dto) {
    	Optional<CustomerAccount> existing = customerAccountRepository.findByCustomerId(customerId);
    	if (existing.isPresent()) {
    		throw new CustomerAccountAlreadyExistsException("Customer account already exists for customer: " + customerId);
    	}
    			
        CustomerAccount customerAccount = modelMapper.map(dto, CustomerAccount.class);
        customerAccount.setCustomerId(customerId);
        customerAccount.setAvailableAmount(BigDecimal.ZERO);
        CustomerAccount saved = customerAccountRepository.saveAndFlush(customerAccount);
        return modelMapper.map(saved, CustomerAccountDTO.class);
    }
    
    public CustomerAccountDTO addMoneyToCustomer(String customerId, BigDecimal amount) {
    	CustomerAccount existing = customerAccountRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CustomerAccountNotFoundException("Customer account not found for customer: " + customerId));
    	
    	existing.setAvailableAmount(existing.getAvailableAmount().add(amount));
    	CustomerAccount saved = customerAccountRepository.saveAndFlush(existing);
    	return modelMapper.map(saved, CustomerAccountDTO.class);
    }
    
    public CustomerAccountDTO subtractMoneyFromCustomer(String customerId, BigDecimal amount) {
    	CustomerAccount existing = customerAccountRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CustomerAccountNotFoundException("Customer account not found for customer: " + customerId));
    	
    	LOG.info("Subtracting " + amount + " from customer " + customerId + " (Amount of money on account before deduction: " + existing.getAvailableAmount() + ")");
    	
    	customerAccountRepository.subtractAmount(customerId, amount);
    	
    	existing = customerAccountRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CustomerAccountNotFoundException("Customer account not found for customer: " + customerId));
    	
    	return modelMapper.map(existing, CustomerAccountDTO.class);
    }
    
    public CustomerAccountDTO findCustomerAccountByCustomerId(String customerId) {
    	CustomerAccount existing = customerAccountRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CustomerAccountNotFoundException("Customer account not found for customer: " + customerId));
 
    	return modelMapper.map(existing, CustomerAccountDTO.class);
    }
}
