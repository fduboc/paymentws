package com.alpian.paymentws.controller;

import com.alpian.paymentws.dto.CreateCardPaymentDTO;
import com.alpian.paymentws.dto.CreateSEPAPaymentDTO;
import com.alpian.paymentws.dto.PaymentIntentDTO;
import com.alpian.paymentws.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for payment-related operations.
 */
@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Payment and PSP attachment endpoints")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/customer/{id}/register")
    @Operation(summary = "Register and attach PSP reference", description = "Create customer at PSP and attach the reference")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "PSP reference attached successfully"),
            @ApiResponse(responseCode = "404", description = "Customer not found"),
            @ApiResponse(responseCode = "409", description = "Customer already has a PSP reference attached")
    })
    public ResponseEntity<Void> registerPspReference(@PathVariable String id) {
    	paymentService.registerCustomer(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/customer/{id}/payCard")
    @Operation(summary = "Create card payment for customer", description = "Create a card payment intent for the customer at the PSP")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment intent created successfully"),
            @ApiResponse(responseCode = "404", description = "Customer not found or has no PSP reference")
    })
    public ResponseEntity<PaymentIntentDTO> createCardPaymentForCustomer(
            @PathVariable String id,
            @RequestBody @Valid CreateCardPaymentDTO dto) {
    	PaymentIntentDTO paymentIntent = paymentService.createCardPaymentForCustomer(id, dto);
        return ResponseEntity.ok(paymentIntent);
    }
    
    @PostMapping("/customer/{id}/paySepa")
    @Operation(summary = "Create SEPA payment for customer", description = "Create a SEPA payment intent for the customer at the PSP")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment intent created successfully"),
            @ApiResponse(responseCode = "404", description = "Customer not found or has no PSP reference"),
            @ApiResponse(responseCode = "400", description = "Provided amount is not valid"),
            @ApiResponse(responseCode = "409", description = "Customer account balance is insufficient")
    })
    public ResponseEntity<PaymentIntentDTO> createSepaPaymentForCustomer(
            @PathVariable String id,
            @RequestBody @Valid CreateSEPAPaymentDTO dto) {
    	PaymentIntentDTO paymentIntent = paymentService.createSEPAPaymentForCustomer(id, dto);
        return ResponseEntity.ok(paymentIntent);
    }
}
