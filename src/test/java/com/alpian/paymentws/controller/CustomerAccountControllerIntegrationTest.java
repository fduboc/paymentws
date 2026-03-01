package com.alpian.paymentws.controller;

import com.alpian.paymentws.domain.Customer;
import com.alpian.paymentws.domain.CustomerAccount;
import com.alpian.paymentws.dto.CustomerAccountDTO;
import com.alpian.paymentws.repository.CustomerAccountRepository;
import com.alpian.paymentws.repository.CustomerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link CustomerAccountController}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CustomerAccountControllerIntegrationTest {

    private static final String BASE_URL = "/api/accounts";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerAccountRepository customerAccountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private Customer savedCustomer;

    @BeforeEach
    void setUp() {
        customerAccountRepository.deleteAll();
        customerRepository.deleteAll();
        savedCustomer = persistCustomer("John", "Doe", "john@example.com");
    }

    // ------------------------------------------------------------------ helpers

    private Customer persistCustomer(String first, String last, String email) {
        Customer c = new Customer();
        c.setFirstName(first);
        c.setLastName(last);
        c.setEmail(email);
        c.setPhoneNumber("+33612345678");
        return customerRepository.save(c);
    }

    private CustomerAccountDTO buildValidAccountDto(String iban) {
        CustomerAccountDTO dto = new CustomerAccountDTO();
        dto.setIban(iban);
        return dto;
    }

    private CustomerAccount persistAccount(String customerId, String iban, BigDecimal amount) {
        CustomerAccount account = new CustomerAccount();
        account.setCustomerId(customerId);
        account.setIban(iban);
        account.setAvailableAmount(amount);
        return customerAccountRepository.save(account);
    }

    // ======================================================================
    //  POST /api/accounts/customer/{id}
    // ======================================================================
    @Nested
    @DisplayName("POST /api/accounts/customer/{id}")
    class CreateCustomerAccount {

        @Test
        @DisplayName("201 – creates account with valid payload")
        void shouldCreateAccount() throws Exception {
            CustomerAccountDTO dto = buildValidAccountDto("CH5362200119938136497");

            mockMvc.perform(post(BASE_URL + "/customer/{id}", savedCustomer.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.customerId").value(savedCustomer.getId()))
                    .andExpect(jsonPath("$.iban").value("CH5362200119938136497"))
                    .andExpect(jsonPath("$.availableAmount").value(comparesEqualTo(0)));

            assertThat(customerAccountRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("201 – availableAmount in payload is ignored, defaults to zero")
        void shouldIgnoreAvailableAmountOnCreate() throws Exception {
            CustomerAccountDTO dto = buildValidAccountDto("CH5362200119938136497");
            dto.setAvailableAmount(new BigDecimal("999.99"));

            mockMvc.perform(post(BASE_URL + "/customer/{id}", savedCustomer.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.availableAmount").value(comparesEqualTo(0)));
        }

        @Test
        @DisplayName("201 – customerId in payload is overridden by path variable")
        void shouldUsePathVariableCustomerId() throws Exception {
            CustomerAccountDTO dto = buildValidAccountDto("CH5362200119938136497");
            dto.setCustomerId("should-be-ignored");

            mockMvc.perform(post(BASE_URL + "/customer/{id}", savedCustomer.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.customerId").value(savedCustomer.getId()));
        }

        @Test
        @DisplayName("400 – missing iban")
        void shouldRejectMissingIban() throws Exception {
            CustomerAccountDTO dto = buildValidAccountDto("");

            mockMvc.perform(post(BASE_URL + "/customer/{id}", savedCustomer.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("409 – duplicate account for same customer")
        void shouldRejectDuplicateAccount() throws Exception {
            persistAccount(savedCustomer.getId(), "CH5362200119938136497", BigDecimal.ZERO);

            CustomerAccountDTO dto = buildValidAccountDto("DE89370400440532013000");

            mockMvc.perform(post(BASE_URL + "/customer/{id}", savedCustomer.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ======================================================================
    //  PUT /api/accounts/customer/{id}/add/{amount}
    // ======================================================================
    @Nested
    @DisplayName("PUT /api/accounts/customer/{id}/add/{amount}")
    class AddMoney {

        @Test
        @DisplayName("200 – adds integer amount")
        void shouldAddIntegerAmount() throws Exception {
            persistAccount(savedCustomer.getId(), "CH5362200119938136497", BigDecimal.ZERO);

            mockMvc.perform(put(BASE_URL + "/customer/{id}/add/{amount}",
                            savedCustomer.getId(), "50"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.customerId").value(savedCustomer.getId()))
                    .andExpect(jsonPath("$.availableAmount").value(comparesEqualTo(50.00)));
        }

        @Test
        @DisplayName("200 – adds decimal amount")
        void shouldAddDecimalAmount() throws Exception {
            persistAccount(savedCustomer.getId(), "CH5362200119938136497", new BigDecimal("100.00"));

            mockMvc.perform(put(BASE_URL + "/customer/{id}/add/{amount}",
                            savedCustomer.getId(), "25.50"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.availableAmount").value(closeTo(125.50, 0.001)));
        }

        @Test
        @DisplayName("200 – cumulative additions")
        void shouldAccumulateMultipleAdditions() throws Exception {
            persistAccount(savedCustomer.getId(), "CH5362200119938136497", BigDecimal.ZERO);

            mockMvc.perform(put(BASE_URL + "/customer/{id}/add/{amount}",
                            savedCustomer.getId(), "10.00"))
                    .andExpect(status().isOk());

            mockMvc.perform(put(BASE_URL + "/customer/{id}/add/{amount}",
                            savedCustomer.getId(), "20.00"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.availableAmount").value(closeTo(30.00, 0.001)));

            // verify persistence
            CustomerAccount reloaded = customerAccountRepository
                    .findByCustomerId(savedCustomer.getId()).orElseThrow();
            assertThat(reloaded.getAvailableAmount()).isEqualByComparingTo(new BigDecimal("30.00"));
        }

        @Test
        @DisplayName("404 – account not found for customer")
        void shouldReturn404WhenAccountNotFound() throws Exception {
            mockMvc.perform(put(BASE_URL + "/customer/{id}/add/{amount}",
                            "non-existent-id", "10.00"))
                    .andExpect(status().isNotFound());
        }
    }
}