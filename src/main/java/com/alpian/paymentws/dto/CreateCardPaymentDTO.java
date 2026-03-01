package com.alpian.paymentws.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * DTO for creating a card payment for a customer.
 */
@Schema(name = "CreateCardPaymentDTO", description = "Request to create a card payment for a customer")
public class CreateCardPaymentDTO {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    @Schema(description = "Payment amount", example = "10.50", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount = BigDecimal.ZERO;

    @NotBlank(message = "Currency is required")
    @Schema(description = "Currency code (e.g. usd)", example = "usd", requiredMode = Schema.RequiredMode.REQUIRED)
    private String currency;
    
    private Boolean immediateCapture = Boolean.TRUE;

    public CreateCardPaymentDTO() {
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

	public Boolean isImmediateCapture() {
		return immediateCapture;
	}

	public void setImmediateCapture(Boolean immediateCapture) {
		this.immediateCapture = immediateCapture;
	}
    
}
