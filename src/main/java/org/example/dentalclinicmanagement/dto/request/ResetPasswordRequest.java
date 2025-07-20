package org.example.dentalclinicmanagement.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotNull(message = "Reset token is required")
    private String token;

    @NotNull(message = "New password is required")
    private String newPassword;
}
