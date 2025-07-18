package org.example.dentalclinicmanagement.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserProfileRequest {

    @Size(min = 1, max = 50, message = "{validation.firstName.size}")
    @Pattern(regexp = "^[a-zA-Zа-яА-ЯіІїЇєЄ'\\s-]+$", message = "{validation.firstName.pattern}")
    private String firstName;

    @Size(min = 1, max = 50, message = "{validation.lastName.size}")
    @Pattern(regexp = "^[a-zA-Zа-яА-ЯіІїЇєЄ'\\s-]+$", message = "{validation.lastName.pattern}")
    private String lastName;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "{validation.phoneNumber.pattern}")
    private String phoneNumber;

    @Email(message = "Email must be valid")
    private String email;
}