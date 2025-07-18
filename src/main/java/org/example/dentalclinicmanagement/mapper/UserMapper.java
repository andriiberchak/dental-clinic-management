package org.example.dentalclinicmanagement.mapper;

import org.example.dentalclinicmanagement.dto.UserDto;
import org.example.dentalclinicmanagement.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toUserDTO(User user);

}