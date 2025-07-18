package org.example.dentalclinicmanagement.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class LoginRequest {
    @NotBlank(message = "{email.blank}")
    @Email
    private String email;

    @NotBlank(message = "{password.blank}")
    private String password;
}