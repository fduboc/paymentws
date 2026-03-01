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
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Concurrency integration tests for {@link PaymentController}.
 *
 * Verifies that the {@code LockManager} correctly prevents duplicate processing
 * when two simultaneous requests arrive for the same customer and amount.
 * Only ONE request should trigger the PSP call, balance deduction, and
 * notification; the second should return the already-created payment intent.
 *
 * NOTE: These tests require the LockManager's locking mechanism to be
 * functional in the test environment (embedded Redis, Redisson mock, etc.).
 */
@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerConcurrencyIntegrationTest {

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

    /**
     * Fire {@code count} concurrent requests using MockMvc and a shared latch
     * so they all start at roughly the same instant.
     */
    private List<MvcResult> fireConcurrentRequests(int count, String url, String jsonBody) throws Exception {
        CountDownLatch readyLatch = new CountDownLatch(count);
        CountDownLatch goLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(count);

        List<Future<MvcResult>> futures = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();       // signal "I'm ready"
                goLatch.await();              // wait for the start gun
                return mockMvc.perform(post(url)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonBody))
                        .andReturn();
            }));
        }

        // wait until all threads are ready, then release them simultaneously
        readyLatch.await(5, TimeUnit.SECONDS);
        goLatch.countDown();

        List<MvcResult> results = new ArrayList<>();
        for (Future<MvcResult> f : futures) {
            results.add(f.get(10, TimeUnit.SECONDS));
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        return results;
    }

    // ======================================================================
    //  Card payment – duplicate simultaneous calls
    // ======================================================================
    @Nested
    @DisplayName("POST /api/payments/customer/{id}/payCard – concurrency")
    class CardPaymentConcurrency {

        @Test
        @DisplayName("Only one PSP call and one notification when two simultaneous card payments are made")
        void shouldProcessOnlyOneCardPayment() throws Exception {
            Customer customer = persistCustomer("John", "Doe", "john@example.com", "cus_psp_concurrent");
            BigDecimal amount = new BigDecimal("75.00");

            // simulate a slow PSP call so the second request arrives while the first is still processing
            when(providerPaymentService.createCardPaymentIntent(
                    eq("cus_psp_concurrent"), eq(amount), eq("chf"), eq(true)))
                    .thenAnswer(invocation -> {
                        Thread.sleep(200); // simulate PSP latency
                        return new CreatePaymentIntentResult("pi_card_concurrent", amount);
                    });

            CreateCardPaymentDTO dto = new CreateCardPaymentDTO();
            dto.setAmount(amount);
            dto.setCurrency("chf");
            dto.setImmediateCapture(true);
            String json = objectMapper.writeValueAsString(dto);

            String url = BASE_URL + "/customer/" + customer.getId() + "/payCard";
            List<MvcResult> results = fireConcurrentRequests(2, url, json);

            // both requests should complete successfully (200)
            for (MvcResult result : results) {
                assertThat(result.getResponse().getStatus()).isEqualTo(200);
            }

            // only ONE payment intent should have been persisted
            assertThat(paymentIntentRepository.count()).isEqualTo(1);

            // the PSP provider should have been called exactly once
            verify(providerPaymentService, times(1))
                    .createCardPaymentIntent(
                            eq("cus_psp_concurrent"), eq(amount), eq("chf"), eq(true));

            // notification should have been sent exactly once
            verify(notificationService, times(1))
                    .notify(eq("payment-topic"), anyString(), any());
        }

        @Test
        @DisplayName("Different amounts are processed independently")
        void shouldProcessDifferentAmountsIndependently() throws Exception {
            Customer customer = persistCustomer("Jane", "Smith", "jane@example.com", "cus_psp_multi");

            BigDecimal amount1 = new BigDecimal("25.00");
            BigDecimal amount2 = new BigDecimal("50.00");

            when(providerPaymentService.createCardPaymentIntent(
                    eq("cus_psp_multi"), eq(amount1), eq("chf"), eq(true)))
                    .thenReturn(new CreatePaymentIntentResult("pi_card_25", amount1));

            when(providerPaymentService.createCardPaymentIntent(
                    eq("cus_psp_multi"), eq(amount2), eq("chf"), eq(true)))
                    .thenReturn(new CreatePaymentIntentResult("pi_card_50", amount2));

            CountDownLatch readyLatch = new CountDownLatch(2);
            CountDownLatch goLatch = new CountDownLatch(1);
            ExecutorService executor = Executors.newFixedThreadPool(2);

            CreateCardPaymentDTO dto1 = new CreateCardPaymentDTO();
            dto1.setAmount(amount1);
            dto1.setCurrency("chf");
            dto1.setImmediateCapture(true);

            CreateCardPaymentDTO dto2 = new CreateCardPaymentDTO();
            dto2.setAmount(amount2);
            dto2.setCurrency("chf");
            dto2.setImmediateCapture(true);

            String url = BASE_URL + "/customer/" + customer.getId() + "/payCard";

            Future<MvcResult> f1 = executor.submit(() -> {
                readyLatch.countDown();
                goLatch.await();
                return mockMvc.perform(post(url)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto1)))
                        .andReturn();
            });

            Future<MvcResult> f2 = executor.submit(() -> {
                readyLatch.countDown();
                goLatch.await();
                return mockMvc.perform(post(url)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto2)))
                        .andReturn();
            });

            readyLatch.await(5, TimeUnit.SECONDS);
            goLatch.countDown();

            assertThat(f1.get(10, TimeUnit.SECONDS).getResponse().getStatus()).isEqualTo(200);
            assertThat(f2.get(10, TimeUnit.SECONDS).getResponse().getStatus()).isEqualTo(200);

            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);

            // two distinct payment intents should exist
            assertThat(paymentIntentRepository.count()).isEqualTo(2);

            // each amount triggered its own PSP call
            verify(providerPaymentService, times(1))
                    .createCardPaymentIntent(eq("cus_psp_multi"), eq(amount1), eq("chf"), eq(true));
            verify(providerPaymentService, times(1))
                    .createCardPaymentIntent(eq("cus_psp_multi"), eq(amount2), eq("chf"), eq(true));

            verify(notificationService, times(2))
                    .notify(eq("payment-topic"), anyString(), any());
        }
    }

    // ======================================================================
    //  SEPA payment – duplicate simultaneous calls
    // ======================================================================
    @Nested
    @DisplayName("POST /api/payments/customer/{id}/paySepa – concurrency")
    class SepaPaymentConcurrency {

        @Test
        @DisplayName("Only one PSP call, one balance deduction, and one notification when two simultaneous SEPA payments are made")
        void shouldProcessOnlyOneSepaPayment() throws Exception {
            Customer customer = persistCustomer("John", "Doe", "john@example.com", "cus_psp_sepa_conc");
            persistAccount(customer.getId(), "CH5362200119938136497", new BigDecimal("1000.00"));
            BigDecimal amount = new BigDecimal("200.00");

            when(providerPaymentService.createSEPAPaymentIntent(
                    eq("cus_psp_sepa_conc"), eq(amount), eq("eur"),
                    eq("John"), eq("Doe"), eq("john@example.com"), eq("+33612345678")))
                    .thenAnswer(invocation -> {
                        Thread.sleep(200); // simulate PSP latency
                        return new CreatePaymentIntentResult("pi_sepa_concurrent", amount);
                    });

            CreateSEPAPaymentDTO dto = new CreateSEPAPaymentDTO();
            dto.setAmount(amount);
            dto.setCurrency("eur");
            dto.setDestinationIban("DE89370400440532013000");
            String json = objectMapper.writeValueAsString(dto);

            String url = BASE_URL + "/customer/" + customer.getId() + "/paySepa";
            List<MvcResult> results = fireConcurrentRequests(2, url, json);

            // both requests should complete successfully
            for (MvcResult result : results) {
                assertThat(result.getResponse().getStatus()).isEqualTo(200);
            }

            // only ONE payment intent should have been persisted
            assertThat(paymentIntentRepository.count()).isEqualTo(1);

            // the PSP provider should have been called exactly once
            verify(providerPaymentService, times(1))
                    .createSEPAPaymentIntent(
                            eq("cus_psp_sepa_conc"), eq(amount), eq("eur"),
                            eq("John"), eq("Doe"), eq("john@example.com"), eq("+33612345678"));

            // balance should have been deducted exactly once (1000 - 200 = 800)
            CustomerAccount reloaded = customerAccountRepository
                    .findByCustomerId(customer.getId()).orElseThrow();
            assertThat(reloaded.getAvailableAmount())
                    .isEqualByComparingTo(new BigDecimal("800.00"));

            // notification should have been sent exactly once
            verify(notificationService, times(1))
                    .notify(eq("payment-topic"), anyString(), any());
        }

        @Test
        @DisplayName("Different destination IBANs are processed independently")
        void shouldProcessDifferentIbansIndependently() throws Exception {
            Customer customer = persistCustomer("Jane", "Smith", "jane@example.com", "cus_psp_sepa_multi");
            persistAccount(customer.getId(), "CH5362200119938136497", new BigDecimal("1000.00"));

            BigDecimal amount = new BigDecimal("100.00");
            String iban1 = "DE89370400440532013000";
            String iban2 = "FR7630006000011234567890189";

            when(providerPaymentService.createSEPAPaymentIntent(
                    eq("cus_psp_sepa_multi"), eq(amount), eq("eur"),
                    anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(new CreatePaymentIntentResult("pi_sepa_iban1", amount))
                    .thenReturn(new CreatePaymentIntentResult("pi_sepa_iban2", amount));

            CountDownLatch readyLatch = new CountDownLatch(2);
            CountDownLatch goLatch = new CountDownLatch(1);
            ExecutorService executor = Executors.newFixedThreadPool(2);

            CreateSEPAPaymentDTO dto1 = new CreateSEPAPaymentDTO();
            dto1.setAmount(amount);
            dto1.setCurrency("eur");
            dto1.setDestinationIban(iban1);

            CreateSEPAPaymentDTO dto2 = new CreateSEPAPaymentDTO();
            dto2.setAmount(amount);
            dto2.setCurrency("eur");
            dto2.setDestinationIban(iban2);

            String url = BASE_URL + "/customer/" + customer.getId() + "/paySepa";

            Future<MvcResult> f1 = executor.submit(() -> {
                readyLatch.countDown();
                goLatch.await();
                return mockMvc.perform(post(url)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto1)))
                        .andReturn();
            });

            Future<MvcResult> f2 = executor.submit(() -> {
                readyLatch.countDown();
                goLatch.await();
                return mockMvc.perform(post(url)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto2)))
                        .andReturn();
            });

            readyLatch.await(5, TimeUnit.SECONDS);
            goLatch.countDown();

            assertThat(f1.get(10, TimeUnit.SECONDS).getResponse().getStatus()).isEqualTo(200);
            assertThat(f2.get(10, TimeUnit.SECONDS).getResponse().getStatus()).isEqualTo(200);

            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);

            // two distinct payment intents
            assertThat(paymentIntentRepository.count()).isEqualTo(2);

            // PSP called twice (different lock keys because different IBANs)
            verify(providerPaymentService, times(2))
                    .createSEPAPaymentIntent(
                            eq("cus_psp_sepa_multi"), eq(amount), eq("eur"),
                            anyString(), anyString(), anyString(), anyString());

            // balance deducted twice: 1000 - 100 - 100 = 800
            CustomerAccount reloaded = customerAccountRepository
                    .findByCustomerId(customer.getId()).orElseThrow();
            assertThat(reloaded.getAvailableAmount())
                    .isEqualByComparingTo(new BigDecimal("800.00"));

            verify(notificationService, times(2))
                    .notify(eq("payment-topic"), anyString(), any());
        }
    }
}