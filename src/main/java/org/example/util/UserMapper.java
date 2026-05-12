package org.example.util;

import lombok.experimental.UtilityClass;
import org.example.dto.UserDTO;
import org.example.entity.User;


@UtilityClass
public class UserMapper {
    public UserDTO convertToDTO(User user) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setUsername(user.getUsername());
        userDTO.setEmail(user.getEmail());
        userDTO.setIsAdmin(user.getIsAdmin());
        return userDTO;
    }
}
