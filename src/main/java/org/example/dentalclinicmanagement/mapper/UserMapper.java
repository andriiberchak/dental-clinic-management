package org.example.dentalclinicmanagement.mapper;

import org.example.dentalclinicmanagement.dto.UserDTO;
import org.example.dentalclinicmanagement.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDTO toUserDTO(User user);

    User toUser(UserDTO userDTO);

}