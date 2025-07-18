package org.example.dentalclinicmanagement.dto;

public record SimpleUserDto(
        String phone,
        String firstName,
        String lastName,
        String email) {
}
