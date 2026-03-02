package com.alpian.paymentws.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.GenericGenerator;

import io.swagger.v3.oas.annotations.media.Schema;

@Entity
@Table(name = "payment_intents")
public class PaymentIntent {

	@Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "id", updatable = false, nullable = false)
    @Schema(description = "Payment Intent unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private String id;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "psp_reference")
    private String pspReference;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "amount_capturable", precision = 19, scale = 4)
    private BigDecimal amountCapturable;
    
    @Column(name = "amount_received", precision = 19, scale = 4)
    private BigDecimal amountReceived;

    @OneToMany(mappedBy = "paymentIntent", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PaymentRequest> paymentRequests = new ArrayList<>();

    @OneToMany(mappedBy = "paymentIntent", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PaymentResponse> paymentResponses = new ArrayList<>();

    public PaymentIntent() {
    }

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

    public String getPspReference() {
        return pspReference;
    }

    public void setPspReference(String pspReference) {
        this.pspReference = pspReference;
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

    public BigDecimal getAmountCapturable() {
        return amountCapturable;
    }

    public void setAmountCapturable(BigDecimal amountCapturable) {
        this.amountCapturable = amountCapturable;
    }
    
    public BigDecimal getAmountReceived() {
		return amountReceived;
	}

	public void setAmountReceived(BigDecimal amountReceived) {
		this.amountReceived = amountReceived;
	}

    public List<PaymentRequest> getPaymentRequests() {
        return paymentRequests;
    }

    public void setPaymentRequests(List<PaymentRequest> paymentRequests) {
        this.paymentRequests = paymentRequests != null ? paymentRequests : new ArrayList<>();
    }

    public List<PaymentResponse> getPaymentResponses() {
        return paymentResponses;
    }

    public void setPaymentResponses(List<PaymentResponse> paymentResponses) {
        this.paymentResponses = paymentResponses != null ? paymentResponses : new ArrayList<>();
    }

	@Override
	public String toString() {
		return "PaymentIntent [id=" + id + ", customerId=" + customerId + ", pspReference=" + pspReference + ", amount="
				+ amount + ", currency=" + currency + ", amountCapturable=" + amountCapturable + ", amountReceived="
				+ amountReceived + ", paymentRequests=" + paymentRequests + ", paymentResponses=" + paymentResponses
				+ "]";
	}
    
    
}
