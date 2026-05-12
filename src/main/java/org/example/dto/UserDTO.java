package org.example.dto;


import lombok.Data;

@Data
public class UserDTO {
    private long id;
    private String username;
    private String email;
    private Boolean isAdmin;
}
