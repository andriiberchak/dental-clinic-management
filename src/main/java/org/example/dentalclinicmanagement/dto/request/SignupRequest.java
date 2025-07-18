package org.example.dentalclinicmanagement.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {
    @NotBlank(message = "{validation.firstName.notBlank}")
    @Size(min = 2, max = 50, message = "{validation.firstName.size}")
    @Pattern(regexp = "^[a-zA-Zа-яА-ЯіІїЇєЄ'\\s-]+$", message = "{validation.firstName.pattern}")
    private String firstName;

    @NotBlank(message = "{validation.lastName.notBlank}")
    @Size(min = 2, max = 50, message = "{validation.lastName.size}")
    @Pattern(regexp = "^[a-zA-Zа-яА-ЯіІїЇєЄ'\\s-]+$", message = "{validation.lastName.pattern}")
    private String lastName;

    @NotBlank(message = "{validation.email.notBlank}")
    @Size(max = 100, message = "{validation.email.size}")
    @Email(message = "{validation.email.invalid}")
    private String email;

    @NotBlank(message = "{validation.password.notBlank}")
    @Size(min = 8, max = 128, message = "{validation.password.size}")
    private String password;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "{validation.phone.pattern}")
    private String phoneNumber;
}