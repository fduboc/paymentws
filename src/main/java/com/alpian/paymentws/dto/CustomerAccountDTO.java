package com.alpian.paymentws.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for Customer Account API representation..
 */
@Schema(name = "CustomerAccountDTO", description = "Customer account data transfer object")
public class CustomerAccountDTO {
	@Schema(description = "Unique id generated at the customer account creation by the system", example = "cb927a10-ffef-4568-9032-69e5b2215fbb")
	private String id;
	
	@Schema(description = "Customer identifier", example = "7e0e227e-0029-4369-80a7-ce5640b18879")
	private String customerId;
	
	@NotBlank(message = "Iban is required")
    @Schema(description = "Customer account iban", example = "CH5362200119938136497")
    private String iban;
	
	@Schema(description = "Amount available in the customer account", example = "10.00")
    private BigDecimal availableAmount;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCustomerId() {
		return customerId;
	}

	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	public String getIban() {
		return iban;
	}

	public void setIban(String iban) {
		this.iban = iban;
	}

	public BigDecimal getAvailableAmount() {
		return availableAmount;
	}

	public void setAvailableAmount(BigDecimal availableAmount) {
		this.availableAmount = availableAmount;
	}
	
}
