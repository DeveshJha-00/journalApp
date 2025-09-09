//package net.engineeringdigest.journalApp.config;
//
//import org.springframework.boot.test.context.TestConfiguration;
//import org.springframework.context.annotation.Bean;
//import org.springframework.mail.javamail.JavaMailSender;
//import org.mockito.Mockito;
//
//@TestConfiguration
//public class TestMailConfig {
//    @Bean
//    public JavaMailSender javaMailSender() {
//        return Mockito.mock(JavaMailSender.class);
//    }
//}

package net.engineeringdigest.journalApp.config;

import net.engineeringdigest.journalApp.services.EmailService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.mockito.Mockito;

@TestConfiguration
public class TestMailConfig {

    @Bean
    @Primary
    public EmailService emailService() {
        EmailService mockEmailService = Mockito.mock(EmailService.class);

        // Mock successful email sending for SendGrid
        Mockito.when(mockEmailService.sendEmail(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(true);

        Mockito.when(mockEmailService.sendHtmlEmail(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(true);

        return mockEmailService;
    }
}