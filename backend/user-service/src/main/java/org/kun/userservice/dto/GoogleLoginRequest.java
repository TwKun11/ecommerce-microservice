package org.kun.userservice.dto;

import lombok.Data;

@Data
public class GoogleLoginRequest {
    private String idToken;
    private boolean rememberMe = false;
}

