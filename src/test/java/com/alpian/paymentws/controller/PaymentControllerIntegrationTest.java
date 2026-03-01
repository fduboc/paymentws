package com.alpian.paymentws.controller;

import com.alpian.paymentws.domain.Customer;
import com.alpian.paymentws.domain.CustomerAccount;
import com.alpian.paymentws.dto.CreateCardPaymentDTO;
import com.alpian.paymentws.dto.CreateSEPAPaymentDTO;
import com.alpian.paymentws.repository.CustomerAccountRepository;
import com.alpian.paymentws.repository.CustomerRepository;
import com.alpian.paymentws.repository.PaymentIntentRepository;
import com.alpian.paymentws.service.NotificationService;
import com.alpian.paymentws.service.psp.CreatePaymentIntentResult;
import com.alpian.paymentws.service.psp.PaymentServiceFactory;
import com.alpian.paymentws.service.psp.ProviderPaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link PaymentController}.
 *
 * External dependencies (PSP provider via {@link PaymentServiceFactory},
 * Kafka via {@link NotificationService}) are mocked with {@code @MockitoBean}
 * so the full Spring request pipeline is exercised while isolating from
 * third-party services.
 *
 * NOTE: If {@code LockManager} depends on Redis (e.g. Redisson), you will also
 * need either a {@code @MockitoBean} for the Redis client or an
 * embedded / test-container Redis instance on the test classpath.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerIntegrationTest {

    private static final String BASE_URL = "/api/payments";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerAccountRepository customerAccountRepository;

    @Autowired
    private PaymentIntentRepository paymentIntentRepository;

    @MockBean
    private PaymentServiceFactory paymentServiceFactory;

    @MockBean
    private NotificationService notificationService;

    private ProviderPaymentService providerPaymentService;

    @BeforeEach
    void setUp() {
        paymentIntentRepository.deleteAll();
        customerAccountRepository.deleteAll();
        customerRepository.deleteAll();

        providerPaymentService = mock(ProviderPaymentService.class);
        when(paymentServiceFactory.getPaymentProviderService()).thenReturn(providerPaymentService);
    }

    // ------------------------------------------------------------------ helpers

    private Customer persistCustomer(String first, String last, String email, String pspRef) {
        Customer c = new Customer();
        c.setFirstName(first);
        c.setLastName(last);
        c.setEmail(email);
        c.setPhoneNumber("+33612345678");
        c.setPspReference(pspRef);
        return customerRepository.save(c);
    }

    private CustomerAccount persistAccount(String customerId, String iban, BigDecimal amount) {
        CustomerAccount a = new CustomerAccount();
        a.setCustomerId(customerId);
        a.setIban(iban);
        a.setAvailableAmount(amount);
        return customerAccountRepository.save(a);
    }

    private CreateCardPaymentDTO buildCardDto(BigDecimal amount, String currency, boolean immediateCapture) {
        CreateCardPaymentDTO dto = new CreateCardPaymentDTO();
        dto.setAmount(amount);
        dto.setCurrency(currency);
        dto.setImmediateCapture(immediateCapture);
        return dto;
    }

    private CreateSEPAPaymentDTO buildSepaDto(BigDecimal amount, String currency, String destIban) {
        CreateSEPAPaymentDTO dto = new CreateSEPAPaymentDTO();
        dto.setAmount(amount);
        dto.setCurrency(currency);
        dto.setDestinationIban(destIban);
        return dto;
    }

    // ======================================================================
    //  POST /api/payments/customer/{id}/register
    // ======================================================================
    @Nested
    @DisplayName("POST /api/payments/customer/{id}/register")
    class RegisterPspReference {

        @Test
        @DisplayName("204 – registers customer at PSP successfully")
        void shouldRegisterCustomer() throws Exception {
            Customer customer = persistCustomer("John", "Doe", "john@example.com", null);

            when(providerPaymentService.createCustomer(
                    anyString(), anyString(), anyString(), anyString(), anyString()))
                    .thenReturn("cus_psp_123");

            mockMvc.perform(post(BASE_URL + "/customer/{id}/register", customer.getId()))
                    .andExpect(status().isNoContent());

            Customer reloaded = customerRepository.findById(customer.getId()).orElseThrow();
            assertThat(reloaded.getPspReference()).isEqualTo("cus_psp_123");

            verify(providerPaymentService).createCustomer(
                    eq(customer.getId()), eq("John"), eq("Doe"),
                    eq("john@example.com"), eq("+33612345678"));
        }

        @Test
        @DisplayName("404 – customer not found")
        void shouldReturn404WhenCustomerNotFound() throws Exception {
            mockMvc.perform(post(BASE_URL + "/customer/{id}/register", "non-existent-id"))
                    .andExpect(status().isNotFound());

            verifyNoInteractions(providerPaymentService);
        }

        @Test
        @DisplayName("409 – customer already has PSP reference")
        void shouldReturn409WhenAlreadyRegistered() throws Exception {
            Customer attached = persistCustomer("Jane", "Smith", "jane@example.com", "cus_existing");

            mockMvc.perform(post(BASE_URL + "/customer/{id}/register", attached.getId()))
                    .andExpect(status().isConflict());

            verifyNoInteractions(providerPaymentService);
        }
    }

    // ======================================================================
    //  POST /api/payments/customer/{id}/payCard
    // ======================================================================
    @Nested
    @DisplayName("POST /api/payments/customer/{id}/payCard")
    class CreateCardPayment {

        @Test
        @DisplayName("200 – creates card payment with immediate capture")
        void shouldCreateCardPaymentWithImmediateCapture() throws Exception {
            Customer customer = persistCustomer("John", "Doe", "john@example.com", "cus_psp_456");
            BigDecimal amount = new BigDecimal("25.00");

            when(providerPaymentService.createCardPaymentIntent(
                    eq("cus_psp_456"), eq(amount), eq("chf"), eq(true)))
                    .thenReturn(new CreatePaymentIntentResult("pi_card_001", amount));

            mockMvc.perform(post(BASE_URL + "/customer/{id}/payCard", customer.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildCardDto(amount, "chf", true))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.customerId").value(customer.getId()))
                    .andExpect(jsonPath("$.pspReference").value("pi_card_001"))
                    .andExpect(jsonPath("$.amount").value(closeTo(25.00, 0.001)))
                    .andExpect(jsonPath("$.currency").value("chf"))
                    .andExpect(jsonPath("$.amountReceived").value(closeTo(25.00, 0.001)))
                    .andExpect(jsonPath("$.amountCapturable").isEmpty());

            assertThat(paymentIntentRepository.count()).isEqualTo(1);
            verify(notificationService).notify(eq("payment-topic"), anyString(), any());
        }

        @Test
        @DisplayName("200 – creates card payment with manual capture (pre-auth)")
        void shouldCreateCardPaymentWithManualCapture() throws Exception {
            Customer customer = persistCustomer("John", "Doe", "john@example.com", "cus_psp_456");
            BigDecimal amount = new BigDecimal("50.00");

            when(providerPaymentService.createCardPaymentIntent(
                    eq("cus_psp_456"), eq(amount), eq("chf"), eq(false)))
                    .thenReturn(new CreatePaymentIntentResult("pi_card_002", amount));

            mockMvc.perform(post(BASE_URL + "/customer/{id}/payCard", customer.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildCardDto(amount, "chf", false))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pspReference").value("pi_card_002"))
                    .andExpect(jsonPath("$.amountCapturable").value(closeTo(50.00, 0.001)))
                    .andExpect(jsonPath("$.amountReceived").isEmpty());
        }

        @Test
        @DisplayName("404 – customer not found")
        void shouldReturn404WhenCustomerNotFound() throws Exception {
            CreateCardPaymentDTO dto = buildCardDto(new BigDecimal("10.00"), "chf", true);

            mockMvc.perform(post(BASE_URL + "/customer/{id}/payCard", "non-existent-id")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isNotFound());

            verifyNoInteractions(providerPaymentService);
        }

        @Test
        @DisplayName("error – customer has no PSP reference")
        void shouldFailWhenNoPspReference() throws Exception {
            Customer customer = persistCustomer("John", "Doe", "john@example.com", null);
            CreateCardPaymentDTO dto = buildCardDto(new BigDecimal("10.00"), "chf", true);

            mockMvc.perform(post(BASE_URL + "/customer/{id}/payCard", customer.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().is4xxClientError());

            verifyNoInteractions(providerPaymentService);
        }

        @Test
        @DisplayName("400 – missing amount")
        void shouldRejectMissingAmount() throws Exception {
            Customer customer = persistCustomer("John", "Doe", "john@example.com", "cus_psp_456");
            CreateCardPaymentDTO dto = new CreateCardPaymentDTO();
            dto.setAmount(null);
            dto.setCurrency("chf");

            mockMvc.perform(post(BASE_URL + "/customer/{id}/payCard", customer.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 – zero amount rejected by validation")
        void shouldRejectZeroAmount() throws Exception {
            Customer customer = persistCustomer("John", "Doe", "john@example.com", "cus_psp_456");
            CreateCardPaymentDTO dto = buildCardDto(BigDecimal.ZERO, "chf", true);

            mockMvc.perform(post(BASE_URL + "/customer/{id}/payCard", customer.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 – missing currency")
        void shouldRejectMissingCurrency() throws Exception {
            Customer customer = persistCustomer("John", "Doe", "john@example.com", "cus_psp_456");
            CreateCardPaymentDTO dto = new CreateCardPaymentDTO();
            dto.setAmount(new BigDecimal("10.00"));
            dto.setCurrency(null);

            mockMvc.perform(post(BASE_URL + "/customer/{id}/payCard", customer.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ======================================================================
    //  POST /api/payments/customer/{id}/paySepa
    // ======================================================================
    @Nested
    @DisplayName("POST /api/payments/customer/{id}/paySepa")
    class CreateSepaPayment {

        @Test
        @DisplayName("200 – creates SEPA payment and deducts balance")
        void shouldCreateSepaPayment() throws Exception {
            Customer customer = persistCustomer("John", "Doe", "john@example.com", "cus_psp_789");
            persistAccount(customer.getId(), "CH5362200119938136497", new BigDecimal("500.00"));
            BigDecimal amount = new BigDecimal("100.00");

            when(providerPaymentService.createSEPAPaymentIntent(
                    eq("cus_psp_789"), eq(amount), eq("eur"),
                    eq("John"), eq("Doe"), eq("john@example.com"), eq("+33612345678")))
                    .thenReturn(new CreatePaymentIntentResult("pi_sepa_001", amount));

            mockMvc.perform(post(BASE_URL + "/customer/{id}/paySepa", customer.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildSepaDto(amount, "eur", "DE89370400440532013000"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.customerId").value(customer.getId()))
                    .andExpect(jsonPath("$.pspReference").value("pi_sepa_001"))
                    .andExpect(jsonPath("$.amount").value(closeTo(100.00, 0.001)))
                    .andExpect(jsonPath("$.currency").value("eur"))
                    .andExpect(jsonPath("$.amountReceived").value(closeTo(100.00, 0.001)));

            // verify balance was deducted
            CustomerAccount reloaded = customerAccountRepository
                    .findByCustomerId(customer.getId()).orElseThrow();
            assertThat(reloaded.getAvailableAmount()).isEqualByComparingTo(new BigDecimal("400.00"));

            assertThat(paymentIntentRepository.count()).isEqualTo(1);
            verify(notificationService).notify(eq("payment-topic"), anyString(), any());
        }

        @Test
        @DisplayName("404 – customer not found")
        void shouldReturn404WhenCustomerNotFound() throws Exception {
            CreateSEPAPaymentDTO dto = buildSepaDto(new BigDecimal("10.00"), "eur", "DE89370400440532013000");

            mockMvc.perform(post(BASE_URL + "/customer/{id}/paySepa", "non-existent-id")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isNotFound());

            verifyNoInteractions(providerPaymentService);
        }

        @Test
        @DisplayName("error – customer has no PSP reference")
        void shouldFailWhenNoPspReference() throws Exception {
            Customer customer = persistCustomer("John", "Doe", "john@example.com", null);
            persistAccount(customer.getId(), "CH5362200119938136497", new BigDecimal("500.00"));
            CreateSEPAPaymentDTO dto = buildSepaDto(new BigDecimal("10.00"), "eur", "DE89370400440532013000");

            mockMvc.perform(post(BASE_URL + "/customer/{id}/paySepa", customer.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().is4xxClientError());

            verifyNoInteractions(providerPaymentService);
        }

        @Test
        @DisplayName("error – insufficient balance")
        void shouldFailWhenInsufficientBalance() throws Exception {
            Customer customer = persistCustomer("John", "Doe", "john@example.com", "cus_psp_789");
            persistAccount(customer.getId(), "CH5362200119938136497", new BigDecimal("5.00"));
            CreateSEPAPaymentDTO dto = buildSepaDto(new BigDecimal("100.00"), "eur", "DE89370400440532013000");

            mockMvc.perform(post(BASE_URL + "/customer/{id}/paySepa", customer.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().is4xxClientError());

            verifyNoInteractions(providerPaymentService);

            // balance should remain untouched
            CustomerAccount reloaded = customerAccountRepository
                    .findByCustomerId(customer.getId()).orElseThrow();
            assertThat(reloaded.getAvailableAmount()).isEqualByComparingTo(new BigDecimal("5.00"));
        }

        @Test
        @DisplayName("error – no account for customer")
        void shouldFailWhenNoAccount() throws Exception {
            Customer customer = persistCustomer("John", "Doe", "john@example.com", "cus_psp_789");
            // no account persisted
            CreateSEPAPaymentDTO dto = buildSepaDto(new BigDecimal("10.00"), "eur", "DE89370400440532013000");

            mockMvc.perform(post(BASE_URL + "/customer/{id}/paySepa", customer.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isNotFound());

            verifyNoInteractions(providerPaymentService);
        }

        @Test
        @DisplayName("400 – zero amount")
        void shouldRejectZeroAmount() throws Exception {
            Customer customer = persistCustomer("John", "Doe", "john@example.com", "cus_psp_789");
            CreateSEPAPaymentDTO dto = buildSepaDto(BigDecimal.ZERO, "eur", "DE89370400440532013000");

            mockMvc.perform(post(BASE_URL + "/customer/{id}/paySepa", customer.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 – missing currency")
        void shouldRejectMissingCurrency() throws Exception {
            Customer customer = persistCustomer("John", "Doe", "john@example.com", "cus_psp_789");
            CreateSEPAPaymentDTO dto = new CreateSEPAPaymentDTO();
            dto.setAmount(new BigDecimal("10.00"));
            dto.setCurrency(null);
            dto.setDestinationIban("DE89370400440532013000");

            mockMvc.perform(post(BASE_URL + "/customer/{id}/paySepa", customer.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 – missing destination IBAN")
        void shouldRejectMissingDestinationIban() throws Exception {
            Customer customer = persistCustomer("John", "Doe", "john@example.com", "cus_psp_789");
            CreateSEPAPaymentDTO dto = new CreateSEPAPaymentDTO();
            dto.setAmount(new BigDecimal("10.00"));
            dto.setCurrency("eur");
            dto.setDestinationIban(null);

            mockMvc.perform(post(BASE_URL + "/customer/{id}/paySepa", customer.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }
    }
}