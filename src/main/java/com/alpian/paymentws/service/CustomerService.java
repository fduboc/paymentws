package com.alpian.paymentws.service;

import com.alpian.paymentws.domain.Customer;
import com.alpian.paymentws.repository.CustomerRepository;
import com.alpian.paymentws.dto.CustomerDTO;
import com.alpian.paymentws.exception.CustomerNotFoundException;
import java.util.List;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

/**
 * Service layer for Customer CRUD and search operations.
 * Maps between CustomerDTO and Customer entity using ModelMapper.
 */
@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final ModelMapper modelMapper;

    public CustomerService(CustomerRepository customerRepository, ModelMapper modelMapper) {
        this.customerRepository = customerRepository;
        this.modelMapper = modelMapper;
    }

    public CustomerDTO createCustomer(CustomerDTO dto) {
        Customer customer = modelMapper.map(dto, Customer.class);
        customer.setPspReference(null);
        Customer saved = customerRepository.save(customer);
        return modelMapper.map(saved, CustomerDTO.class);
    }

    public CustomerDTO updateCustomer(String id, CustomerDTO dto) {
        Customer existing = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + id));

        existing.setFirstName(dto.getFirstName());
        existing.setLastName(dto.getLastName());
        existing.setEmail(dto.getEmail());
        existing.setPhoneNumber(dto.getPhoneNumber());

        Customer saved = customerRepository.save(existing);
        return modelMapper.map(saved, CustomerDTO.class);
    }

    public void deleteCustomer(String id) {
        if (!customerRepository.existsById(id)) {
            throw new CustomerNotFoundException("Customer not found: " + id);
        }
        customerRepository.deleteById(id);
    }

    public CustomerDTO getCustomerById(String id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + id));
        return modelMapper.map(customer, CustomerDTO.class);
    }

    public List<CustomerDTO> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(c -> modelMapper.map(c, CustomerDTO.class))
                .toList();
    }

    public List<CustomerDTO> getCustomersByFirstAndLastName(String firstName, String lastName) {
        return customerRepository.findByFirstNameAndLastName(firstName, lastName).stream()
                .map(c -> modelMapper.map(c, CustomerDTO.class))
                .toList();
    }

    /**
     * Load the customer and return its PSP reference.
     *
     * @param customerId the customer id
     * @return the customer's pspReference
     * @throws CustomerNotFoundException if the customer does not exist
     */
    public String getCustomerPSPReference(String id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + id));
        return customer.getPspReference();
    }
    
    public CustomerDTO attachPspReference(String id, String pspReference) {
    	Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + id));
    	
    	customer.setPspReference(pspReference);
        Customer saved = customerRepository.save(customer);
        return modelMapper.map(saved, CustomerDTO.class);
    }

    
}
