package com.spring.JavaT.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

/**
 * Sends emails asynchronously using Spring's {@link JavaMailSender}.
 *
 * <p>All public methods are annotated with {@code @Async("emailTaskExecutor")}
 * so they execute on the dedicated email thread pool defined in
 * {@link com.spring.JavaT.config.AsyncConfig}. The calling thread returns
 * immediately — email delivery happens in the background.
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
    // Generic send
    // -------------------------------------------------------------------------

    /**
     * Sends an email asynchronously.
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
    // Verification email
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

        String body = buildVerificationEmailBody(firstName, confirmUrl);

        send(EmailRequest.builder()
                .to(toEmail)
                .toName(firstName)
                .subject("Verify your " + mailProperties.getFromName() + " account")
                .body(body)
                .html(true)
                .build());
    }

    // -------------------------------------------------------------------------
    // Password reset email
    // -------------------------------------------------------------------------

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

        String body = buildPasswordResetEmailBody(firstName, resetUrl);

        send(EmailRequest.builder()
                .to(toEmail)
                .toName(firstName)
                .subject("Reset your " + mailProperties.getFromName() + " password")
                .body(body)
                .html(true)
                .build());
    }

    // -------------------------------------------------------------------------
    // HTML templates
    // -------------------------------------------------------------------------

    private String buildVerificationEmailBody(String firstName, String confirmUrl) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; color: #333; max-width: 600px; margin: auto;">
                  <h2>Welcome to %s, %s!</h2>
                  <p>Please verify your email address by clicking the button below.</p>
                  <p>
                    <a href="%s"
                       style="display:inline-block; padding:12px 24px; background:#4F46E5;
                              color:#fff; text-decoration:none; border-radius:6px;">
                      Verify Email
                    </a>
                  </p>
                  <p>Or copy this link into your browser:</p>
                  <p><a href="%s">%s</a></p>
                  <p>This link expires in 24 hours.</p>
                  <hr/>
                  <p style="font-size:12px; color:#999;">
                    If you did not create an account, you can safely ignore this email.
                  </p>
                </body>
                </html>
                """.formatted(
                mailProperties.getFromName(), firstName,
                confirmUrl, confirmUrl, confirmUrl
        );
    }

    private String buildPasswordResetEmailBody(String firstName, String resetUrl) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; color: #333; max-width: 600px; margin: auto;">
                  <h2>Password Reset Request</h2>
                  <p>Hi %s,</p>
                  <p>We received a request to reset your password. Click the button below to choose a new one.</p>
                  <p>
                    <a href="%s"
                       style="display:inline-block; padding:12px 24px; background:#4F46E5;
                              color:#fff; text-decoration:none; border-radius:6px;">
                      Reset Password
                    </a>
                  </p>
                  <p>Or copy this link into your browser:</p>
                  <p><a href="%s">%s</a></p>
                  <p><strong>This link expires in 15 minutes.</strong></p>
                  <hr/>
                  <p style="font-size:12px; color:#999;">
                    If you did not request a password reset, you can safely ignore this email.
                    Your password will not be changed.
                  </p>
                </body>
                </html>
                """.formatted(firstName, resetUrl, resetUrl, resetUrl);
    }
}
