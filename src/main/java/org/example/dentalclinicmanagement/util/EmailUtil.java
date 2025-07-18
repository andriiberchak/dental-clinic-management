package org.example.dentalclinicmanagement.util;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class EmailUtil {
    private final JavaMailSender javaMailSender;

    public void sendPasswordResetEmail(String to, String resetUrl) {
        log.debug("Sending password reset email to: {}", to);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Password Reset Request");
        message.setText("Click the link to reset your password: " + resetUrl);
        javaMailSender.send(message);

        log.info("Password reset email sent successfully to: {}", to);
    }
}
