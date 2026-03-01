package com.alpian.paymentws.service.psp;

import com.alpian.paymentws.exception.PaymentProviderTransactionException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.net.RequestOptions;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentIntentCreateParams.CaptureMethod;
import com.stripe.param.PaymentMethodCreateParams;
import com.stripe.param.PaymentMethodCreateParams.BillingDetails.Address;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

/**
 * Stripe implementation of ProviderPaymentService.
 */
@Service
public class StripePaymentService implements ProviderPaymentService {
	private static final String MD_CUSTOMER_ID = "MD_CUSTOMER_ID";
	private static final String TEST_IBAN = "CH9300762011623852957";
	
    @Override
    public String createCustomer(String customerId, String firstName, String lastName, String email, String phoneNumber) throws PaymentProviderTransactionException {
        try {
        	String apiKey = System.getenv("STRIPE_API_KEY");
        	CustomerCreateParams.Builder builder = CustomerCreateParams.builder()
                    .setEmail(email)
                    .setPhone(phoneNumber)
                    .setName(firstName + " " + lastName)
                    .putMetadata(MD_CUSTOMER_ID, customerId);

            CustomerCreateParams createParams = builder.build();

            RequestOptions options = RequestOptions.builder()
                    .setApiKey(apiKey)
                    .build();

            Customer customer = Customer.create(createParams, options);
            return customer.getId();
        } catch (Exception e) {
        	throw new PaymentProviderTransactionException("Error creating customer on Stripe", e);
        }
    }
    
    public CreatePaymentIntentResult createSEPAPaymentIntent(String customerPspReference, BigDecimal amount, String currency, String firstName, String lastName, String email, String phoneNumber) {
    	try {
    		String apiKey = System.getenv("STRIPE_API_KEY");
            RequestOptions requestOptions = RequestOptions.builder()
                    .setApiKey(apiKey)
                    .build();

            long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();
            
            PaymentMethod pm = createSepaPaymentMethod(
            		TEST_IBAN,
            		firstName + " " + lastName,
                    email,
                    phoneNumber,
                    requestOptions
            );
            
            PaymentIntent paymentIntentNew = createAndConfirmSepaPayment(
                    pm.getId(),
                    amountInCents,
                    "eur",  // force euro for SEPA on Stripe
                    customerPspReference,
                    requestOptions
            );
            
            return new CreatePaymentIntentResult(
                    paymentIntentNew.getId(),
                    amount);
    	} catch (Exception e) {
    		throw new PaymentProviderTransactionException("Error creating SEPA payment intent on Stripe", e);
    	}
    }
    
    private PaymentIntent createAndConfirmSepaPayment(String paymentMethodId, long amountInCents, String currency, String customerId, RequestOptions requestOptions) throws StripeException {				
    	PaymentIntentCreateParams createParams = PaymentIntentCreateParams.builder()
			.setAmount(amountInCents)
			.setCurrency(currency)
			.setPaymentMethod(paymentMethodId)
			.addAllPaymentMethodType(List.of("sepa_debit"))
			.setConfirm(true) // confirm immediately
			.setCustomer(customerId)
			.setMandateData(
		            PaymentIntentCreateParams.MandateData.builder()
		                .setCustomerAcceptance(
		                    PaymentIntentCreateParams.MandateData.CustomerAcceptance.builder()
		                        .setType(PaymentIntentCreateParams.MandateData
		                                .CustomerAcceptance.Type.ONLINE)
		                        .setOnline(
		                            PaymentIntentCreateParams.MandateData
		                                .CustomerAcceptance.Online.builder()
		                                .setIpAddress("127.0.0.1")
		                                .setUserAgent("test-agent")
		                                .build()
		                        )
		                        .build()
		                )
		                .build()
		        )
			.build();
			
		return PaymentIntent.create(createParams, requestOptions);
	}
    
    private PaymentMethod createSepaPaymentMethod(String iban, String name, String email, String phoneNumber, RequestOptions requestOptions)
            throws StripeException {

        PaymentMethodCreateParams params = PaymentMethodCreateParams.builder()
                .setType(PaymentMethodCreateParams.Type.SEPA_DEBIT)
                .setSepaDebit(
                        PaymentMethodCreateParams.SepaDebit.builder()
                                .setIban(iban)
                                .build()
                )
                .setBillingDetails(
                        PaymentMethodCreateParams.BillingDetails.builder()
                                .setName(name)
                                .setEmail(email)
                                .setPhone(phoneNumber)
                                .setAddress(new Address.Builder()
                                		.setCity("fakeCity").setCountry("CH")
                                		.setLine1("fakeCountry").setLine2("second line")
                                		.setState("fakeState").build())
                                .build())
                .build();

        return PaymentMethod.create(params, requestOptions);
    }

    @Override
    public CreatePaymentIntentResult createCardPaymentIntent(String customerPspReference, BigDecimal amount, String currency, boolean immediateCapture) {
        try {
            String apiKey = System.getenv("STRIPE_API_KEY");
            RequestOptions requestOptions = RequestOptions.builder()
                    .setApiKey(apiKey)
                    .build();

            long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();

            PaymentIntentCreateParams.AutomaticPaymentMethods automaticPaymentMethods =
                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                            .setEnabled(true)
                            .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                            .build();

            PaymentIntentCreateParams paymentIntentParams = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(currency)
                    .setCustomer(customerPspReference)
                    .setPaymentMethod("pm_card_visa")
                    .setConfirm(true)
                    .setCaptureMethod(immediateCapture? CaptureMethod.AUTOMATIC : CaptureMethod.MANUAL)
                    .setAutomaticPaymentMethods(automaticPaymentMethods)
                    .build();

            PaymentIntent paymentIntentNew = PaymentIntent.create(paymentIntentParams, requestOptions);

            BigDecimal amountReceivedOrCapturable = immediateCapture
            		? BigDecimal.valueOf(paymentIntentNew.getAmountReceived()).divide(BigDecimal.valueOf(100))
                    : BigDecimal.valueOf(paymentIntentNew.getAmountCapturable()).divide(BigDecimal.valueOf(100));

            return new CreatePaymentIntentResult(
                    paymentIntentNew.getId(),
                    amountReceivedOrCapturable);
        } catch (StripeException e) {
            throw new PaymentProviderTransactionException("Error creating card payment intent on Stripe", e);
        }
    }
}
