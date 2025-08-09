package org.kun.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendPasswordResetEmail(String to, String resetToken) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Password Reset Request");
            message.setText(buildPasswordResetEmailText(resetToken));
            
            mailSender.send(message);
            log.info("Password reset email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", to, e);
            throw new RuntimeException("Failed to send password reset email");
        }
    }

    private String buildPasswordResetEmailText(String resetToken) {
        return """
                Hello,
                
                You have requested to reset your password. Please use the following token to reset your password:
                
                Reset Token: %s
                
                This token will expire in 1 hour.
                
                If you did not request this password reset, please ignore this email.
                
                Best regards,
                E-commerce Team
                """.formatted(resetToken);
    }
}
