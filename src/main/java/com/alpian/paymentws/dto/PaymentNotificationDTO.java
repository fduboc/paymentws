package com.alpian.paymentws.dto;

import java.math.BigDecimal;

public class PaymentNotificationDTO {

    private String customerId;
    private String firstName;
    private String lastName;
    private String email;
    private String customerAccountIban;
    private String destinationIban;
    private BigDecimal paymentAmount;
    private String currency;
    
	public PaymentNotificationDTO(String customerId, String firstName, String lastName, String email,
			String customerAccountIban, String destinationIban, BigDecimal paymentAmount, String currency) {
		super();
		this.customerId = customerId;
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
		this.customerAccountIban = customerAccountIban;
		this.destinationIban = destinationIban;
		this.paymentAmount = paymentAmount;
		this.currency = currency;
	}
	
	public String getCustomerId() {
		return customerId;
	}
	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}
	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getCustomerAccountIban() {
		return customerAccountIban;
	}
	public void setCustomerAccountIban(String customerAccountIban) {
		this.customerAccountIban = customerAccountIban;
	}
	public String getDestinationIban() {
		return destinationIban;
	}
	public void setDestinationIban(String destinationIban) {
		this.destinationIban = destinationIban;
	}
	public BigDecimal getPaymentAmount() {
		return paymentAmount;
	}
	public void setPaymentAmount(BigDecimal paymentAmount) {
		this.paymentAmount = paymentAmount;
	}
	public String getCurrency() {
		return currency;
	}
	public void setCurrency(String currency) {
		this.currency = currency;
	}
	
	@Override
	public String toString() {
		return "PaymentNotificationDTO [customerId=" + customerId + ", firstName=" + firstName + ", lastName="
				+ lastName + ", email=" + email + ", customerAccountIban=" + customerAccountIban + ", destinationIban="
				+ destinationIban + ", paymentAmount=" + paymentAmount + ", currency=" + currency + "]";
	}
}
