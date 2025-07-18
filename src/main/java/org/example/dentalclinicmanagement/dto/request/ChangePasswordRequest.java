package org.example.dentalclinicmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    @NotBlank(message = "{update.current.password.required}")
    private String currentPassword;

    @NotBlank(message = "{update.new.password.required}")
    private String newPassword;

    @NotBlank(message = "{update.confirm.password.required}")
    private String confirmPassword;
}
