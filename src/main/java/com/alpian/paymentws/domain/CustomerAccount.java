package com.alpian.paymentws.domain;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

/**
 * Customer account entity for persistence and REST representation.
 */
@Entity
@Table(name = "customer_account")
@Schema(name = "Customer Account", description = "Customer account")
public class CustomerAccount {
	@Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "id", updatable = false, nullable = false)
    @Schema(description = "Customer account unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private String id;
	
	@Column(name = "customer_id", nullable = false, unique = true)
    private String customerId;
	
	@Column(nullable = false)
    @NotBlank(message = "Iban is required")
    @Schema(description = "Customer account iban", example = "CH5362200119938136497")
	private String iban;
	
	@Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal availableAmount;
	
	@CreationTimestamp
    @Column(name = "creation_date", nullable = false, updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    @Schema(description = "Creation timestamp", accessMode = Schema.AccessMode.READ_ONLY)
    private Instant creationDate;

    @UpdateTimestamp
    @Column(name = "update_date", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    @Schema(description = "Last update timestamp", accessMode = Schema.AccessMode.READ_ONLY)
    private Instant updateDate;

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

	public Instant getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Instant creationDate) {
		this.creationDate = creationDate;
	}

	public Instant getUpdateDate() {
		return updateDate;
	}

	public void setUpdateDate(Instant updateDate) {
		this.updateDate = updateDate;
	}
	
	
}
