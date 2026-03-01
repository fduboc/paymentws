package com.alpian.paymentws.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.alpian.paymentws.util.LockKeyProducer;

/**
 * DTO for creating a SEPA payment for a customer.
 */
@Schema(name = "CreateSEPAPaymentDTO", description = "Request to create a SEPA payment for a customer")
public class CreateSEPAPaymentDTO {
	@NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    @Schema(description = "Payment amount", example = "10.50", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount = BigDecimal.ZERO;

    @NotBlank(message = "Currency is required")
    @Schema(description = "Currency code (e.g. usd)", example = "usd", requiredMode = Schema.RequiredMode.REQUIRED)
    private String currency;
    
    @NotBlank(message = "Destination iban is required")
    @Schema(description = "Destination iban", requiredMode = Schema.RequiredMode.REQUIRED)
    private String destinationIban;
    
    public CreateSEPAPaymentDTO() {
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

	public String getDestinationIban() {
		return destinationIban;
	}

	public void setDestinationIban(String destinationIban) {
		this.destinationIban = destinationIban;
	}
}
