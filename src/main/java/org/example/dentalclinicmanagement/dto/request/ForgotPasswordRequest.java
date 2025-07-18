package org.example.dentalclinicmanagement.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ForgotPasswordRequest {
    @Email(message = "{validation.email.invalid}")
    @NotNull(message = "{validation.email.required}")
    String email;
}
