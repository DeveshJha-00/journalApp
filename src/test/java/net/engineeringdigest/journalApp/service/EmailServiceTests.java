package net.engineeringdigest.journalApp.service;

import net.engineeringdigest.journalApp.config.TestMailConfig;
import net.engineeringdigest.journalApp.services.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.ArgumentMatchers.any;

@ActiveProfiles("dev")
@Import(TestMailConfig.class)
public class EmailServiceTests {

    @InjectMocks
    private EmailService emailService;

    @Mock
    private JavaMailSender javaMailSender;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

//    @Disabled("Disabled for CI/CD - mocked test, no real email sent")
    @Test
    void testSendMail() {

        Mockito.doNothing().when(javaMailSender).send(any(SimpleMailMessage.class));

        emailService.sendEmail(
                "ashujhaaaa@gmail.com",
                "Return to sentiment analysis",
                "This is a test email to verify the email service functionality."
        );

        Mockito.verify(javaMailSender, Mockito.times(1))
                .send(any(SimpleMailMessage.class));
    }


}
