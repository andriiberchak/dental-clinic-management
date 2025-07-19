package org.example.dentalclinicmanagement.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CalendarEventDto {
    private String eventId;
    private String title;
    private String description;
    private String status;
    private String url;
}