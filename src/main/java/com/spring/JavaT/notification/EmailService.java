package com.spring.JavaT.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Sends emails asynchronously using Spring's {@link JavaMailSender}.
 *
 * <p>All public methods are annotated with {@code @Async("emailTaskExecutor")}
 * so they execute on the dedicated email thread pool defined in
 * {@link com.spring.JavaT.config.AsyncConfig}. The calling thread returns
 * immediately — email delivery happens in the background.
 *
 * <p>HTML templates live in {@code src/main/resources/templates/email/}.
 * Variables are substituted using a simple {@code {{placeholder}}} syntax
 * via {@link #loadTemplate(String, Map)} — no template engine dependency needed.
 *
 * <p>Failures are logged but not re-thrown to the caller. If you need
 * retry logic, replace the catch block with a message queue (e.g. RabbitMQ).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    // -------------------------------------------------------------------------
    // Generic send — the reusable core
    // -------------------------------------------------------------------------

    /**
     * Sends an email asynchronously.
     *
     * <p>This is the single entry point for all email delivery. Every named
     * method below (verification, password reset, etc.) builds an
     * {@link EmailRequest} and delegates here.
     *
     * @param request all email data (to, subject, body, html flag)
     */
    @Async("emailTaskExecutor")
    public void send(EmailRequest request) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(
                    new InternetAddress(mailProperties.getFrom(), mailProperties.getFromName())
            );

            if (request.getToName() != null) {
                helper.setTo(new InternetAddress(request.getTo(), request.getToName()));
            } else {
                helper.setTo(request.getTo());
            }

            helper.setSubject(request.getSubject());
            helper.setText(request.getBody(), request.isHtml());

            mailSender.send(message);
            log.info("Email sent to [{}] subject=[{}]", request.getTo(), request.getSubject());

        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Failed to send email to [{}]: {}", request.getTo(), e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Named email types — add new ones here as the app grows
    // -------------------------------------------------------------------------

    /**
     * Sends an account verification email with a clickable confirmation link.
     *
     * @param toEmail   recipient email address
     * @param firstName recipient's first name for personalisation
     * @param token     the verification token (appended to the confirmation URL)
     */
    @Async("emailTaskExecutor")
    public void sendVerificationEmail(String toEmail, String firstName, String token) {
        String confirmUrl = mailProperties.getBaseUrl()
                + "/api/v1/auth/verify-email?token=" + token;

        String body = loadTemplate("verification.html", Map.of(
                "appName",    mailProperties.getFromName(),
                "firstName",  firstName,
                "confirmUrl", confirmUrl
        ));

        send(EmailRequest.builder()
                .to(toEmail)
                .toName(firstName)
                .subject("Verify your " + mailProperties.getFromName() + " account")
                .body(body)
                .html(true)
                .build());
    }

    /**
     * Sends a password reset email with a time-limited reset link.
     *
     * @param toEmail   recipient email address
     * @param firstName recipient's first name for personalisation
     * @param token     the password reset token (appended to the reset URL)
     */
    @Async("emailTaskExecutor")
    public void sendPasswordResetEmail(String toEmail, String firstName, String token) {
        String resetUrl = mailProperties.getBaseUrl()
                + "/api/v1/auth/reset-password?token=" + token;

        String body = loadTemplate("password-reset.html", Map.of(
                "appName",   mailProperties.getFromName(),
                "firstName", firstName,
                "resetUrl",  resetUrl
        ));

        send(EmailRequest.builder()
                .to(toEmail)
                .toName(firstName)
                .subject("Reset your " + mailProperties.getFromName() + " password")
                .body(body)
                .html(true)
                .build());
    }

    // -------------------------------------------------------------------------
    // Template loader
    // -------------------------------------------------------------------------

    /**
     * Loads an HTML template from {@code classpath:templates/email/<name>}
     * and substitutes all {@code {{key}}} placeholders with the provided values.
     *
     * <p>This is intentionally simple — no template engine dependency.
     * If you need conditionals, loops, or inheritance, add Thymeleaf or
     * Freemarker and replace this method.
     *
     * @param templateName filename inside {@code templates/email/} (e.g. {@code "verification.html"})
     * @param variables    map of placeholder name → replacement value
     * @return the rendered HTML string
     */
    private String loadTemplate(String templateName, Map<String, String> variables) {
        try {
            ClassPathResource resource = new ClassPathResource("templates/email/" + templateName);
            String content = resource.getContentAsString(StandardCharsets.UTF_8);

            for (Map.Entry<String, String> entry : variables.entrySet()) {
                content = content.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }

            return content;

        } catch (IOException e) {
            log.error("Failed to load email template [{}]: {}", templateName, e.getMessage());
            // Fallback: return a plain-text body so the email still goes out
            return variables.getOrDefault("body", "Please contact support.");
        }
    }
}
