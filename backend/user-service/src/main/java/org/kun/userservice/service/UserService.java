package org.kun.userservice.service;

import org.keycloak.representations.account.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final Keycloak keycloakAdmin;

    @Value("${keycloak.realm}")
    private String realm;

    public UserService(Keycloak keycloakAdmin) {
        this.keycloakAdmin = keycloakAdmin;
    }

    public void registerUser(String username, String email, String password) {
        // Tạo user representation
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(email);
        user.setEnabled(true);

        // Gửi yêu cầu tạo user
        Response response = keycloakAdmin.realm(realm).users().create(user);
        if (response.getStatus() != 201) {
            throw new RuntimeException("Failed to create user: " + response.getStatus());
        }

        // Lấy userId mới tạo
        String userId = CreatedResponseUtil.getCreatedId(response);

        // Đặt password cho user
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);

        keycloakAdmin.realm(realm).users().get(userId).resetPassword(credential);

        // Gán role USER
        RoleRepresentation userRole = keycloakAdmin.realm(realm).roles().get("USER").toRepresentation();
        keycloakAdmin.realm(realm).users().get(userId).roles().realmLevel().add(Collections.singletonList(userRole));
    }
}
