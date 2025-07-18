package org.example.dentalclinicmanagement.dto;

public record ImportReport(String fileName,
                           int created,
                           int updated,
                           int skipped) {}
