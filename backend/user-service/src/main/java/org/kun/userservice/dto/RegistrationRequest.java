package org.kun.userservice.dto;

import lombok.Data;

@Data
public class RegistrationRequest {
    private String username;
    private String email;
    private String password;
    private String firstName;  // thêm
    private String lastName;   // thêm
}