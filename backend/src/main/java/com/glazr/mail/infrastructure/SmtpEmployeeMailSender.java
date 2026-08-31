package com.glazr.mail.infrastructure;

import com.glazr.mail.application.EmployeeInvitationMessage;
import com.glazr.mail.application.EmployeeMailSender;
import com.glazr.mail.application.PasswordResetMessage;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class SmtpEmployeeMailSender implements EmployeeMailSender {
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    private final JavaMailSender mailSender;
    private final ResourceLoader resources;
    private final PublicBaseUrl publicBaseUrl;
    private final String fromAddress;
    private final String fromName;

    public SmtpEmployeeMailSender(JavaMailSender mailSender, ResourceLoader resources,
                                  @Value("${glazr.public-base-url}") String publicBaseUrl,
                                  @Value("${spring.mail.from.address}") String fromAddress,
                                  @Value("${spring.mail.from.name}") String fromName) {
        this.mailSender = mailSender;
        this.resources = resources;
        this.publicBaseUrl = new PublicBaseUrl(publicBaseUrl);
        this.fromAddress = fromAddress;
        this.fromName = fromName;
    }

    @Override
    public void sendInvitation(EmployeeInvitationMessage message) {
        String link = publicBaseUrl.invitationLink(message.rawToken());
        send(message.recipientEmail(), "Seu convite para acessar o Glazr", message.displayName(), link,
                DATE_FORMAT.format(message.expiresAt()), "employee-invitation");
    }

    @Override
    public void sendPasswordReset(PasswordResetMessage message) {
        String link = publicBaseUrl.passwordResetLink(message.rawToken());
        send(message.recipientEmail(), "Redefinição de senha do Glazr", message.displayName(), link,
                DATE_FORMAT.format(message.expiresAt()), "password-reset");
    }

    private void send(String recipient, String subject, String displayName, String link,
                      String expiresAt, String template) {
        try {
            String text = render(template + ".txt", displayName, link, expiresAt, false);
            String html = render(template + ".html", displayName, link, expiresAt, true);
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
            helper.setTo(recipient);
            helper.setFrom(fromAddress, fromName);
            helper.setSubject(subject);
            helper.setText(text, html);
            mailSender.send(mimeMessage);
        } catch (MessagingException | IOException exception) {
            throw new MailDeliveryException(exception);
        }
    }

    private String render(String filename, String name, String link, String expiresAt, boolean html) throws IOException {
        Resource resource = resources.getResource("classpath:templates/mail/" + filename);
        String template = resource.getContentAsString(StandardCharsets.UTF_8);
        String safeName = html ? HtmlUtils.htmlEscape(name) : name;
        String safeLink = html ? HtmlUtils.htmlEscape(link) : link;
        return template.replace("{{name}}", safeName).replace("{{link}}", safeLink)
                .replace("{{expiresAt}}", expiresAt);
    }

    public static final class MailDeliveryException extends RuntimeException {
        public MailDeliveryException(Throwable cause) { super("Falha ao enviar e-mail", cause); }
    }
}
