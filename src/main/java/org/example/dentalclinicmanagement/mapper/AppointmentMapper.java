package org.example.dentalclinicmanagement.mapper;

import org.example.dentalclinicmanagement.dto.AppointmentDto;
import org.example.dentalclinicmanagement.dto.TimeSlotDto;
import org.example.dentalclinicmanagement.model.Appointment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface AppointmentMapper {
    
    @Mapping(source = "dentist.id", target = "dentistId")
    @Mapping(source = "client.id", target = "clientId")
    @Mapping(source = "appointment", target = "dentistName", qualifiedByName = "mapDentistName")
    @Mapping(source = "appointment", target = "clientName", qualifiedByName = "mapClientName")
    AppointmentDto toDto(Appointment appointment);
    
    @Mapping(source = "id", target = "appointmentId")
    @Mapping(source = "appointmentTime", target = "slotTime")
    @Mapping(source = "client.id", target = "clientId")
    @Mapping(source = "client.email", target = "clientName")
    @Mapping(source = "client.firstName", target = "firstName")
    @Mapping(source = "client.lastName", target = "lastName")
    TimeSlotDto toTimeSlotDto(Appointment appointment);
    
    @Named("mapDentistName")
    default String mapDentistName(Appointment appointment) {
        if (appointment.getDentist() == null) return null;
        return appointment.getDentist().getFirstName() + " " + appointment.getDentist().getLastName();
    }
    
    @Named("mapClientName")
    default String mapClientName(Appointment appointment) {
        if (appointment.getClient() == null) return null;
        return appointment.getClient().getFirstName() + " " + appointment.getClient().getLastName();
    }
}