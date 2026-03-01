package com.alpian.paymentws.controller;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alpian.paymentws.dto.CustomerAccountDTO;
import com.alpian.paymentws.service.CustomerAccountService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST controller for Customer Account CRUD and search operations.
 */
@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Customer Accounts", description = "Customer accounts management endpoints")
@Validated
public class CustomerAccountController {
	private final CustomerAccountService customerAccountService;

    public CustomerAccountController(CustomerAccountService customerAccountService) {
        this.customerAccountService = customerAccountService;
    }
    
    @PostMapping("/customer/{id}")
    @Operation(summary = "Create customer account", description = "Create a new customer account")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Customer account created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    public ResponseEntity<CustomerAccountDTO> createCustomerAccount(
    		@PathVariable String id,
    		@RequestBody @Valid CustomerAccountDTO customerAccountDTO) {
    	CustomerAccountDTO created = customerAccountService.createCustomerAccount(id, customerAccountDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/customer/{id}/add/{amount:.+}")
    @Operation(summary = "Add money to customer account", description = "Add money to an existing customer account")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Amount added correctly to customer account"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "404", description = "Customer account not found")
    })
    public ResponseEntity<CustomerAccountDTO> updateCustomer(
            @PathVariable String id,
            @PathVariable BigDecimal amount) {
    	CustomerAccountDTO updated = customerAccountService.addMoneyToCustomer(id, amount);
        return ResponseEntity.ok(updated);
    }
}
