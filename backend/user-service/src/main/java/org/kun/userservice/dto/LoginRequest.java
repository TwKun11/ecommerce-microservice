package org.kun.userservice.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
    private boolean rememberMe = false;
}


