package com.alpian.paymentws.service;

import com.alpian.paymentws.domain.PaymentIntent;
import com.alpian.paymentws.dto.CreateCardPaymentDTO;
import com.alpian.paymentws.dto.CreateSEPAPaymentDTO;
import com.alpian.paymentws.dto.CustomerAccountDTO;
import com.alpian.paymentws.dto.CustomerDTO;
import com.alpian.paymentws.dto.PaymentIntentDTO;
import com.alpian.paymentws.dto.PaymentNotificationDTO;
import com.alpian.paymentws.exception.CustomerAlreadyAttachedException;
import com.alpian.paymentws.exception.CustomerNotAttachedException;
import com.alpian.paymentws.exception.CustomerNotFoundException;
import com.alpian.paymentws.exception.InsufficientBalanceException;
import com.alpian.paymentws.exception.PaymentAmountException;
import com.alpian.paymentws.repository.PaymentIntentRepository;
import com.alpian.paymentws.service.psp.CreatePaymentIntentResult;
import com.alpian.paymentws.service.psp.PaymentServiceFactory;
import com.alpian.paymentws.service.psp.ProviderPaymentService;
import com.alpian.paymentws.util.LockManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.apache.commons.lang3.tuple.Pair;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Payment service that delegates customer creation and payment intent creation to the configured PSP implementation.
 */
@Transactional
@Service
public class PaymentService {
	private final Logger LOG = LoggerFactory.getLogger(PaymentService.class);
	
	@Autowired
    private AutowireCapableBeanFactory beanFactory;
	
	private static final String PAYMENT_TOPIC = "payment-topic";
	private static final String DATE_TIME_UTC_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private final PaymentServiceFactory paymentServiceFactory;
    private final CustomerService customerService;
    private final CustomerAccountService customerAccountService;
    private final NotificationService notificationService;
    private final PaymentIntentRepository paymentIntentRepository;
    private final ModelMapper modelMapper;

    public PaymentService(PaymentServiceFactory paymentServiceFactory,
            CustomerService customerService,
            CustomerAccountService customerAccountService,
            NotificationService notificationService,
            PaymentIntentRepository paymentIntentRepository,
            ModelMapper modelMapper) {
        this.paymentServiceFactory = paymentServiceFactory;
        this.customerService = customerService;
        this.customerAccountService = customerAccountService;
        this.notificationService = notificationService;
        this.paymentIntentRepository = paymentIntentRepository;
        this.modelMapper = modelMapper;
    }
    
    /**
     * Attach a PSP reference to a customer by creating the customer at the PSP
     * and storing the returned reference.
     *
     * @param id customer id
     * @throws CustomerNotFoundException       if the customer does not exist
     * @throws CustomerAlreadyAttachedException if the customer already has a pspReference
     */
    public void registerCustomer(String customerId) {
    	CustomerDTO customerDTO = customerService.getCustomerById(customerId);
    	
        if (customerDTO.getPspReference() != null && !customerDTO.getPspReference().isBlank()) {
            throw new CustomerAlreadyAttachedException("Customer already has a PSP reference attached");
        }

        String pspReference = createCustomer(
        		customerDTO.getId(),
        		customerDTO.getFirstName(),
        		customerDTO.getLastName(),
        		customerDTO.getEmail(),
        		customerDTO.getPhoneNumber());
        
        customerService.attachPspReference(customerId, pspReference);
    }

    /**
     * Create a customer at the payment service provider.
     */
    private String createCustomer(String customerId, String firstName, String lastName, String email, String phoneNumber) {
        ProviderPaymentService pspPaymentService = paymentServiceFactory.getPaymentProviderService();
        return pspPaymentService.createCustomer(customerId, firstName, lastName, email, phoneNumber);
    }

    /**
     * Create a card payment for a customer: resolve PSP reference, create payment intent at PSP, persist PaymentIntent.
     *
     * @param customerId the customer id
     * @param dto        amount, currency and immediateCapture
     * @return the created PaymentIntent entity
     * @throws CustomerNotFoundException if the customer does not exist or has no PSP reference
     */
    public PaymentIntentDTO createCardPaymentForCustomer(String customerId, CreateCardPaymentDTO dto) {
    	if (dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
    		throw new PaymentAmountException("Amount provided is invalid: " + dto.getAmount());
    	}
    	
    	CustomerDTO customerDTO = customerService.getCustomerById(customerId);
    	
        String pspReference = customerDTO.getPspReference();
        if (pspReference == null || pspReference.isBlank()) {
            throw new CustomerNotAttachedException("Customer has no PSP reference attached: " + customerId);
        }
        
    	// This redis lock should avoid handling the same payment requirement more than once handling concurrency issues
    	LockManager<PaymentIntentDTO, Pair<String, CreateCardPaymentDTO>> lockManager = new LockManager<PaymentIntentDTO, Pair<String, CreateCardPaymentDTO>>() {

			@Override
			protected String getKey(Pair<String, CreateCardPaymentDTO> context) {
				return "payment-card-lock_" + customerId + "_" + dto.getAmount();
			}

			@Override
			protected PaymentIntentDTO checkIfCreated(Pair<String, CreateCardPaymentDTO> context) {
				// TODO To be improved adding other aspects
				Optional<PaymentIntent> opt = paymentIntentRepository.findAll().stream().filter(pi -> pi.getCustomerId().equals(customerId)
						&& pi.getAmount().compareTo(context.getRight().getAmount()) == 0).findFirst();
				PaymentIntent paymentIntent = opt.orElse(null);
				return paymentIntent != null? modelMapper.map(paymentIntent, PaymentIntentDTO.class) : null;
			}
    	};
    	
    	beanFactory.autowireBean(lockManager);
    	
    	PaymentIntentDTO paymentIntentDTO = lockManager.withLock(Pair.of(customerId, dto), () -> {
            ProviderPaymentService provider = paymentServiceFactory.getPaymentProviderService();
            CreatePaymentIntentResult result = provider.createCardPaymentIntent(pspReference, dto.getAmount(), dto.getCurrency(), dto.isImmediateCapture());

            PaymentIntent paymentIntent = new PaymentIntent();
            paymentIntent.setCustomerId(customerId);
            paymentIntent.setPspReference(result.getPspPaymentIntentId());
            paymentIntent.setAmount(dto.getAmount());
            paymentIntent.setCurrency(dto.getCurrency());
            if (dto.isImmediateCapture()) {
            	paymentIntent.setAmountReceived(result.getAmount());
            } else {
            	paymentIntent.setAmountCapturable(result.getAmount());
            }
            
            paymentIntent = paymentIntentRepository.saveAndFlush(paymentIntent);
            
            PaymentNotificationDTO paymentNotificationDTO = new PaymentNotificationDTO(customerId, customerDTO.getFirstName(),
            		customerDTO.getLastName(), customerDTO.getEmail(),
            		null, null, dto.getAmount(), dto.getCurrency());
            
            notificationService.notify(PAYMENT_TOPIC, "payment-card_" + customerId + "_" + 
            								LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_TIME_UTC_FORMAT)), paymentNotificationDTO);
            
            return modelMapper.map(paymentIntent, PaymentIntentDTO.class);
    	});	
    	
    	return paymentIntentDTO;
    }
    
    /**
     * Create a SEPA payment for a customer: resolve PSP reference, create payment intent at PSP, persist PaymentIntent.
     *
     * @param customerId the customer id
     * @param dto        amount, currency, destinationIban and destinationBic
     * @return the created PaymentIntent entity
     * @throws CustomerNotFoundException if the customer does not exist or has no PSP reference
     */
    public PaymentIntentDTO createSEPAPaymentForCustomer(String customerId, CreateSEPAPaymentDTO dto) {
		CustomerDTO customerDTO = customerService.getCustomerById(customerId);
    	
    	if (dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
    		throw new PaymentAmountException("Amount provided is invalid: " + dto.getAmount());
    	}
    	
    	// Account balance check
    	CustomerAccountDTO customerAccountDTO = customerAccountService.findCustomerAccountByCustomerId(customerId);
    	if (customerAccountDTO.getAvailableAmount().compareTo(dto.getAmount()) < 0) {
    		throw new InsufficientBalanceException("Amount provided " + dto.getAmount() + " is above account balance " + customerAccountDTO.getAvailableAmount());
    	}
    	
    	String pspReference = customerDTO.getPspReference();    	
        if (pspReference == null || pspReference.isBlank()) {
            throw new CustomerNotAttachedException("Customer has no PSP reference attached: " + customerId);
        }
        
        LOG.info("SEPA payment for customer " + customerId + ": " + dto);
        
    	// This redis lock should avoid handling the same payment requirement more than once handling concurrency issues
    	LockManager<PaymentIntentDTO, Pair<String, CreateSEPAPaymentDTO>> lockManager = new LockManager<PaymentIntentDTO, Pair<String, CreateSEPAPaymentDTO>>() {

			@Override
			protected String getKey(Pair<String, CreateSEPAPaymentDTO> context) {
				return "payment-sepa-lock_" + customerId + "_" + dto.getDestinationIban() + "_" + dto.getAmount();
			}

			@Override
			protected PaymentIntentDTO checkIfCreated(Pair<String, CreateSEPAPaymentDTO> context) {
				// TODO To be improved adding other aspects
				Optional<PaymentIntent> opt = paymentIntentRepository.findAll().stream().filter(pi -> pi.getCustomerId().equals(customerId)
						&& pi.getAmount().compareTo(context.getRight().getAmount()) == 0).findFirst();
				PaymentIntent paymentIntent = opt.orElse(null);
				return paymentIntent != null? modelMapper.map(paymentIntent, PaymentIntentDTO.class) : null;
			}

    	};
    	
    	beanFactory.autowireBean(lockManager);
    	
    	PaymentIntentDTO paymentIntentDTO = lockManager.withLock(Pair.of(customerId, dto), () -> {	
            ProviderPaymentService provider = paymentServiceFactory.getPaymentProviderService();
            CreatePaymentIntentResult result = provider.createSEPAPaymentIntent(pspReference, dto.getAmount(), dto.getCurrency(), customerDTO.getFirstName(),
            		customerDTO.getLastName(), customerDTO.getEmail(), customerDTO.getPhoneNumber());          
            
            PaymentIntent paymentIntent = new PaymentIntent();
            paymentIntent.setCustomerId(customerId);
            paymentIntent.setPspReference(result.getPspPaymentIntentId());
            paymentIntent.setAmount(dto.getAmount());
            paymentIntent.setCurrency(dto.getCurrency());
            paymentIntent.setAmountReceived(result.getAmount());
            
            paymentIntent = paymentIntentRepository.saveAndFlush(paymentIntent);
            
            LOG.info("Created payment intent: " + paymentIntent);
            
            customerAccountService.subtractMoneyFromCustomer(customerId, dto.getAmount());
            
            PaymentNotificationDTO paymentNotificationDTO = new PaymentNotificationDTO(customerId, customerDTO.getFirstName(),
            		customerDTO.getLastName(), customerDTO.getEmail(),
            		customerAccountDTO.getIban(), dto.getDestinationIban(), dto.getAmount(), dto.getCurrency());
            
            notificationService.notify(PAYMENT_TOPIC, "payment-sepa_" + customerId + "_" + dto.getDestinationIban() + "_" + 
            								LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_TIME_UTC_FORMAT)), paymentNotificationDTO);
            
            return modelMapper.map(paymentIntent, PaymentIntentDTO.class);
    	});	
    	
    	return paymentIntentDTO;
    }
}
