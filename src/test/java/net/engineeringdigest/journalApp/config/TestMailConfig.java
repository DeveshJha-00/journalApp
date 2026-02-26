package net.engineeringdigest.journalApp.config;

import net.engineeringdigest.journalApp.services.EmailService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestMailConfig {

    @Bean
    @Primary
    public EmailService emailService() {

        EmailService mockEmailService = Mockito.mock(EmailService.class);

        // Always return success for tests (Brevo mocked)
        Mockito.when(mockEmailService.sendEmail(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyString()
        )).thenReturn(true);

        Mockito.when(mockEmailService.sendHtmlEmail(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyString()
        )).thenReturn(true);

        return mockEmailService;
    }
}