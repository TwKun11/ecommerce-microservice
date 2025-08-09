import org.keycloak.*;

import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    @Value("${keycloak.realm}")
    private String realm;

    private final Keycloak keycloak;

    public AuthController(Keycloak keycloak) {
        this.keycloak = keycloak;
    }

    // Đăng ký tài khoản
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegistrationRequest request) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setEnabled(true);
        user.setEmailVerified(false);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.getPassword());
        credential.setTemporary(false);

        user.setCredentials(Collections.singletonList(credential));

        UsersResource usersResource = keycloak.realm(realm).users();
        Response response = usersResource.create(user);

        if (response.getStatus() == 201) {
            // Gán role user mặc định
            String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
            UserResource userResource = usersResource.get(userId);
            userResource.roles().realmLevel().add(Collections.singletonList(
                    keycloak.realm(realm).roles().get("user").toRepresentation()
            ));

            return ResponseEntity.ok().body(Map.of("message", "User registered successfully"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Registration failed"));
        }
    }

    // Lấy thông tin profile
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal OidcUser principal) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", principal.getName());
        profile.put("username", principal.getPreferredUsername());
        profile.put("email", principal.getEmail());
        profile.put("firstName", principal.getGivenName());
        profile.put("lastName", principal.getFamilyName());

        return ResponseEntity.ok(profile);
    }

    // Đổi mật khẩu
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@AuthenticationPrincipal OidcUser principal,
                                            @RequestBody ChangePasswordRequest request) {
        UsersResource usersResource = keycloak.realm(realm).users();
        UserResource userResource = usersResource.get(principal.getName());

        CredentialRepresentation newCredential = new CredentialRepresentation();
        newCredential.setType(CredentialRepresentation.PASSWORD);
        newCredential.setValue(request.getNewPassword());
        newCredential.setTemporary(false);

        userResource.resetPassword(newCredential);

        return ResponseEntity.ok().body(Map.of("message", "Password changed successfully"));
    }

    // Model classes
    public static class RegistrationRequest {
        private String username;
        private String email;
        private String password;
        // getters & setters
    }

    public static class ChangePasswordRequest {
        private String newPassword;
        // getters & setters
    }
}