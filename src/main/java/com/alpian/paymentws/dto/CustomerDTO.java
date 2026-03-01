package com.alpian.paymentws.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

/**
 * DTO for Customer API representation..
 */
@Schema(name = "CustomerDTO", description = "Customer data transfer object")
public class CustomerDTO {
	
	@Schema(description = "Unique id generated at the customer creation by the system", example = "cb927a10-ffef-4568-9032-69e5b2215fbb")
	private String id;

    @NotBlank(message = "First name is required")
    @Schema(description = "Customer first name", example = "John")
    private String firstName;

    @Schema(description = "PSP reference")
    private String pspReference;

    @NotBlank(message = "Last name is required")
    @Schema(description = "Customer last name", example = "Doe")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Schema(description = "Customer email address", example = "john.doe@example.com")
    private String email;

    @Schema(description = "Customer phone number")
    private String phoneNumber;

    @Schema(description = "Creation timestamp", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant creationDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    @Schema(description = "Last update timestamp", accessMode = Schema.AccessMode.READ_ONLY)
    private Instant updateDate;

    public CustomerDTO() {
    }
    
    public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getPspReference() {
        return pspReference;
    }

    public void setPspReference(String pspReference) {
        this.pspReference = pspReference;
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

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
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
