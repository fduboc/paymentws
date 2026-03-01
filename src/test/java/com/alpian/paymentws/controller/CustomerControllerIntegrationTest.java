package com.alpian.paymentws.controller;

import com.alpian.paymentws.domain.Customer;
import com.alpian.paymentws.dto.CustomerDTO;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link CustomerController}.
 *
 * Uses a real Spring context with an embedded database (H2 / Testcontainers)
 * so that the full request pipeline — validation, serialization, service,
 * repository — is exercised.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CustomerControllerIntegrationTest {

    private static final String BASE_URL = "/api/customers";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @BeforeEach
    void setUp() {
        customerRepository.deleteAll();
    }

    // ------------------------------------------------------------------ helpers

    private CustomerDTO buildValidDto(String first, String last, String email) {
        CustomerDTO dto = new CustomerDTO();
        dto.setFirstName(first);
        dto.setLastName(last);
        dto.setEmail(email);
        dto.setPhoneNumber("+33612345678");
        return dto;
    }

    private Customer persistCustomer(String first, String last, String email) {
        Customer c = new Customer();
        c.setFirstName(first);
        c.setLastName(last);
        c.setEmail(email);
        c.setPhoneNumber("+33612345678");
        return customerRepository.save(c);
    }

    // ======================================================================
    //  POST /api/customers
    // ======================================================================
    @Nested
    @DisplayName("POST /api/customers")
    class CreateCustomer {

        @Test
        @DisplayName("201 – creates customer with valid payload")
        void shouldCreateCustomer() throws Exception {
            CustomerDTO dto = buildValidDto("John", "Doe", "john@example.com");

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$.lastName").value("Doe"))
                    .andExpect(jsonPath("$.email").value("john@example.com"))
                    .andExpect(jsonPath("$.phoneNumber").value("+33612345678"))
                    .andExpect(jsonPath("$.creationDate").isNotEmpty())
                    .andExpect(jsonPath("$.updateDate").isNotEmpty())
                    .andExpect(jsonPath("$.pspReference").isEmpty());

            assertThat(customerRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("201 – pspReference in payload is ignored on creation")
        void shouldIgnorePspReferenceOnCreate() throws Exception {
            CustomerDTO dto = buildValidDto("Jane", "Smith", "jane@example.com");
            dto.setPspReference("psp_should_be_ignored");

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.pspReference").isEmpty());
        }

        @Test
        @DisplayName("400 – missing firstName")
        void shouldRejectMissingFirstName() throws Exception {
            CustomerDTO dto = buildValidDto(null, "Doe", "john@example.com");
            // Also try blank
            dto.setFirstName("");

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 – missing lastName")
        void shouldRejectMissingLastName() throws Exception {
            CustomerDTO dto = buildValidDto("John", "", "john@example.com");

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 – missing email")
        void shouldRejectMissingEmail() throws Exception {
            CustomerDTO dto = buildValidDto("John", "Doe", "");

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 – invalid email format")
        void shouldRejectInvalidEmail() throws Exception {
            CustomerDTO dto = buildValidDto("John", "Doe", "not-an-email");

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ======================================================================
    //  GET /api/customers/{id}
    // ======================================================================
    @Nested
    @DisplayName("GET /api/customers/{id}")
    class GetCustomerById {

        @Test
        @DisplayName("200 – returns existing customer")
        void shouldReturnCustomer() throws Exception {
            Customer saved = persistCustomer("John", "Doe", "john@example.com");

            mockMvc.perform(get(BASE_URL + "/{id}", saved.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(saved.getId()))
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$.lastName").value("Doe"))
                    .andExpect(jsonPath("$.email").value("john@example.com"));
        }

        @Test
        @DisplayName("404 – unknown id")
        void shouldReturn404ForUnknownId() throws Exception {
            mockMvc.perform(get(BASE_URL + "/{id}", "non-existent-id"))
                    .andExpect(status().isNotFound());
        }
    }

    // ======================================================================
    //  GET /api/customers
    // ======================================================================
    @Nested
    @DisplayName("GET /api/customers")
    class GetAllCustomers {

        @Test
        @DisplayName("200 – returns empty list when no customers exist")
        void shouldReturnEmptyList() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("200 – returns all persisted customers")
        void shouldReturnAllCustomers() throws Exception {
            persistCustomer("John", "Doe", "john@example.com");
            persistCustomer("Jane", "Smith", "jane@example.com");

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[*].firstName", containsInAnyOrder("John", "Jane")));
        }
    }

    // ======================================================================
    //  PUT /api/customers/{id}
    // ======================================================================
    @Nested
    @DisplayName("PUT /api/customers/{id}")
    class UpdateCustomer {

        @Test
        @DisplayName("200 – updates all mutable fields")
        void shouldUpdateCustomer() throws Exception {
            Customer saved = persistCustomer("John", "Doe", "john@example.com");

            CustomerDTO update = buildValidDto("Jonathan", "Updated", "jonathan@example.com");
            update.setPhoneNumber("+33699999999");

            mockMvc.perform(put(BASE_URL + "/{id}", saved.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(update)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(saved.getId()))
                    .andExpect(jsonPath("$.firstName").value("Jonathan"))
                    .andExpect(jsonPath("$.lastName").value("Updated"))
                    .andExpect(jsonPath("$.email").value("jonathan@example.com"))
                    .andExpect(jsonPath("$.phoneNumber").value("+33699999999"));

            // verify persistence
            Customer reloaded = customerRepository.findById(saved.getId()).orElseThrow();
            assertThat(reloaded.getFirstName()).isEqualTo("Jonathan");
        }

        @Test
        @DisplayName("404 – unknown id")
        void shouldReturn404ForUnknownId() throws Exception {
            CustomerDTO update = buildValidDto("Jonathan", "Updated", "jonathan@example.com");

            mockMvc.perform(put(BASE_URL + "/{id}", "non-existent-id")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(update)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("400 – invalid payload")
        void shouldRejectInvalidPayload() throws Exception {
            Customer saved = persistCustomer("John", "Doe", "john@example.com");
            CustomerDTO update = buildValidDto("", "", "bad-email");

            mockMvc.perform(put(BASE_URL + "/{id}", saved.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(update)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ======================================================================
    //  DELETE /api/customers/{id}
    // ======================================================================
    @Nested
    @DisplayName("DELETE /api/customers/{id}")
    class DeleteCustomer {

        @Test
        @DisplayName("204 – deletes existing customer")
        void shouldDeleteCustomer() throws Exception {
            Customer saved = persistCustomer("John", "Doe", "john@example.com");

            mockMvc.perform(delete(BASE_URL + "/{id}", saved.getId()))
                    .andExpect(status().isNoContent());

            assertThat(customerRepository.existsById(saved.getId())).isFalse();
        }

        @Test
        @DisplayName("404 – unknown id")
        void shouldReturn404ForUnknownId() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/{id}", "non-existent-id"))
                    .andExpect(status().isNotFound());
        }
    }

    // ======================================================================
    //  GET /api/customers/search?firstName=…&lastName=…
    // ======================================================================
    @Nested
    @DisplayName("GET /api/customers/search")
    class SearchByName {

        @Test
        @DisplayName("200 – finds matching customers")
        void shouldFindByFirstAndLastName() throws Exception {
            persistCustomer("John", "Doe", "john1@example.com");
            persistCustomer("John", "Doe", "john2@example.com");
            persistCustomer("Jane", "Smith", "jane@example.com");

            mockMvc.perform(get(BASE_URL + "/search")
                            .param("firstName", "John")
                            .param("lastName", "Doe"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[*].email",
                            containsInAnyOrder("john1@example.com", "john2@example.com")));
        }

        @Test
        @DisplayName("200 – returns empty list when no match")
        void shouldReturnEmptyWhenNoMatch() throws Exception {
            persistCustomer("John", "Doe", "john@example.com");

            mockMvc.perform(get(BASE_URL + "/search")
                            .param("firstName", "Nobody")
                            .param("lastName", "Here"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("400 – missing firstName parameter")
        void shouldRejectMissingFirstName() throws Exception {
            mockMvc.perform(get(BASE_URL + "/search")
                            .param("lastName", "Doe"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 – missing lastName parameter")
        void shouldRejectMissingLastName() throws Exception {
            mockMvc.perform(get(BASE_URL + "/search")
                            .param("firstName", "John"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 – blank firstName parameter")
        void shouldRejectBlankFirstName() throws Exception {
            mockMvc.perform(get(BASE_URL + "/search")
                            .param("firstName", "")
                            .param("lastName", "Doe"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ======================================================================
    //  Serialization checks
    // ======================================================================
    @Nested
    @DisplayName("JSON serialization")
    class JsonSerialization {

        @Test
        @DisplayName("timestamps are formatted as ISO-8601 with millis")
        void shouldFormatTimestamps() throws Exception {
            Customer saved = persistCustomer("John", "Doe", "john@example.com");

            mockMvc.perform(get(BASE_URL + "/{id}", saved.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.creationDate",
                            matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z")))
                    .andExpect(jsonPath("$.updateDate",
                            matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z")));
        }
    }
}