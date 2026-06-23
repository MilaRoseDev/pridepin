package com.pridepin.pridepin.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sends transactional emails (verification, welcome). Methods are @Async so they do not block the HTTP request.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${app.base-url}")
    private String baseUrl;

    /** Sends an HTML email with a link to verify the user's email address. Runs asynchronously. */
    @Async
    public void sendVerificationEmail(String to, String username, String token) {
        String verifyUrl = baseUrl + "/api/v1/auth/verify?token=" + token;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject("Verify your PridePin account");
            helper.setText(buildVerificationHtml(username, verifyUrl), true);
            mailSender.send(message);
            log.info("Verification email sent to {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send verification email to {}: {}", to, e.getMessage());
        }
    }

    /** Sends a welcome email after successful verification. Runs asynchronously. */
    @Async
    public void sendWelcomeEmail(String to, String username) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject("Welcome to PridePin!");
            helper.setText(buildWelcomeHtml(username), true);
            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Failed to send welcome email to {}: {}", to, e.getMessage());
        }
    }

    // ── HTML templates ─────────────────────────────────────────────────────

    /** Builds the HTML body for the verification email with a clickable link. */
    private String buildVerificationHtml(String username, String verifyUrl) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
            <body style="margin:0;padding:0;background:#0d0d1a;font-family:'Segoe UI',sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="padding:2rem 1rem;">
                <tr><td align="center">
                  <table width="520" cellpadding="0" cellspacing="0"
                         style="background:#120020;border:1px solid #3a1060;border-radius:12px;overflow:hidden;">

                    <!-- Header -->
                    <tr>
                      <td style="background:linear-gradient(90deg,#1a0030,#12002a);padding:1.5rem 2rem;border-bottom:1px solid #3a1060;">
                        <h1 style="margin:0;font-size:1.4rem;color:#c77dff;">🏳️‍⚧️ PridePin</h1>
                        <p style="margin:0.25rem 0 0;color:#7050a0;font-size:0.85rem;">Safe Space Mapping</p>
                      </td>
                    </tr>

                    <!-- Body -->
                    <tr>
                      <td style="padding:2rem;">
                        <p style="margin:0 0 1rem;color:#e0c0ff;font-size:1rem;">
                          Hi <strong style="color:#c77dff;">%s</strong>,
                        </p>
                        <p style="margin:0 0 1.5rem;color:#b090d0;line-height:1.6;">
                          Thanks for joining PridePin. Please verify your email address to complete
                          your registration and start exploring safe spaces.
                        </p>

                        <!-- CTA button -->
                        <table cellpadding="0" cellspacing="0" style="margin-bottom:1.5rem;">
                          <tr>
                            <td style="background:#7b2d8b;border-radius:8px;">
                              <a href="%s"
                                 style="display:inline-block;padding:0.8rem 2rem;color:#fff;
                                        text-decoration:none;font-weight:700;font-size:0.95rem;">
                                Verify Email Address
                              </a>
                            </td>
                          </tr>
                        </table>

                        <p style="margin:0 0 0.5rem;color:#5040a0;font-size:0.8rem;">
                          Or paste this link into your browser:
                        </p>
                        <p style="margin:0 0 1.5rem;word-break:break-all;">
                          <a href="%s" style="color:#7b2d8b;font-size:0.8rem;">%s</a>
                        </p>
                        <p style="margin:0;color:#4030a0;font-size:0.78rem;">
                          This link expires in 24 hours. If you didn't create a PridePin account,
                          you can safely ignore this email.
                        </p>
                      </td>
                    </tr>

                    <!-- Footer -->
                    <tr>
                      <td style="padding:1rem 2rem;border-top:1px solid #2a0040;background:#0d0015;">
                        <p style="margin:0;color:#3a2060;font-size:0.75rem;text-align:center;">
                          PridePin — A safe space for everyone 🏳️‍⚧️🏳️‍🌈
                        </p>
                      </td>
                    </tr>

                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(username, verifyUrl, verifyUrl, verifyUrl);
    }

    /** Builds the HTML body for the welcome email. */
    private String buildWelcomeHtml(String username) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <body style="margin:0;padding:2rem 1rem;background:#0d0d1a;font-family:'Segoe UI',sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0">
                <tr><td align="center">
                  <table width="520" cellpadding="0" cellspacing="0"
                         style="background:#120020;border:1px solid #3a1060;border-radius:12px;overflow:hidden;">
                    <tr>
                      <td style="background:linear-gradient(90deg,#1a0030,#12002a);padding:1.5rem 2rem;border-bottom:1px solid #3a1060;">
                        <h1 style="margin:0;font-size:1.4rem;color:#c77dff;">🏳️‍⚧️ Welcome to PridePin!</h1>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:2rem;">
                        <p style="color:#e0c0ff;">Hi <strong style="color:#c77dff;">%s</strong>,</p>
                        <p style="color:#b090d0;line-height:1.6;">
                          Your email has been verified and your account is ready. Start exploring
                          and adding safe spaces on the map!
                        </p>
                        <p style="color:#4030a0;font-size:0.78rem;">PridePin — A safe space for everyone 🏳️‍⚧️🏳️‍🌈</p>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(username);
    }
}
