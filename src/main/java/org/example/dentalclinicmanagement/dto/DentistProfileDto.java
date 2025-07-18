package org.example.dentalclinicmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DentistProfileDto {
    private Long dentistId;
    private String description;
    private int experience;
    private String firstName;
    private String lastName;
    private String photoUrl;
}
