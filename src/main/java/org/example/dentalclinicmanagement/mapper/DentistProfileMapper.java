package org.example.dentalclinicmanagement.mapper;

import org.example.dentalclinicmanagement.dto.DentistProfileDto;
import org.example.dentalclinicmanagement.model.DentistProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DentistProfileMapper {

    @Mapping(source = "dentist.id", target = "dentistId")
    @Mapping(source = "dentist.firstName", target = "firstName")
    @Mapping(source = "dentist.lastName", target = "lastName")
    @Mapping(source = "yearsOfExperience", target = "experience")
    DentistProfileDto toDto(DentistProfile dentistProfile);
}
