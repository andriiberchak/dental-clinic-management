package org.example.dentalclinicmanagement.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotNull(message = "{reset.token.required}")
    private String token;

    @NotNull(message = "{reset.password.new.required}")
    private String newPassword;
}
