package com.glazr.mail.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class SmtpConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withConfiguration(AutoConfigurations.of(MailSenderAutoConfiguration.class));

    @Test
    void configuresAuthenticatedStartTlsDeliveryForGmail() {
        contextRunner.withSystemProperties(
                        "SMTP_HOST=smtp.gmail.com",
                        "SMTP_PORT=587",
                        "SMTP_USERNAME=mailer@example.com",
                        "SMTP_PASSWORD=app-password",
                        "SMTP_AUTH_ENABLED=true",
                        "SMTP_TLS_ENABLED=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(JavaMailSenderImpl.class);
                    JavaMailSenderImpl sender = context.getBean(JavaMailSenderImpl.class);
                    Properties properties = sender.getJavaMailProperties();

                    assertThat(sender.getHost()).isEqualTo("smtp.gmail.com");
                    assertThat(sender.getPort()).isEqualTo(587);
                    assertThat(properties.getProperty("mail.smtp.auth")).isEqualTo("true");
                    assertThat(properties.getProperty("mail.smtp.starttls.enable")).isEqualTo("true");
                    assertThat(properties.getProperty("mail.smtp.starttls.required")).isEqualTo("true");
                    assertThat(properties.getProperty("mail.smtp.connectiontimeout")).isEqualTo("5000");
                    assertThat(properties.getProperty("mail.smtp.timeout")).isEqualTo("5000");
                    assertThat(properties.getProperty("mail.smtp.writetimeout")).isEqualTo("5000");
                    assertThat(context.getEnvironment().getProperty("spring.mail.from.address"))
                            .isEqualTo("mailer@example.com");
                });
    }
}
