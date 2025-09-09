////package net.engineeringdigest.journalApp.service;
////
////import net.engineeringdigest.journalApp.config.TestMailConfig;
////import net.engineeringdigest.journalApp.services.EmailService;
////import org.junit.jupiter.api.BeforeEach;
////import org.junit.jupiter.api.Disabled;
////import org.junit.jupiter.api.Test;
////import org.mockito.InjectMocks;
////import org.mockito.Mock;
////import org.mockito.Mockito;
////import org.mockito.MockitoAnnotations;
////import org.springframework.context.annotation.Import;
////import org.springframework.mail.javamail.JavaMailSender;
////import org.springframework.mail.SimpleMailMessage;
////import org.springframework.test.context.ActiveProfiles;
////
////import static org.mockito.ArgumentMatchers.any;
////
////@ActiveProfiles("dev")
////@Import(TestMailConfig.class)
////public class EmailServiceTests {
////
////    @InjectMocks
////    private EmailService emailService;
////
////    @Mock
////    private JavaMailSender javaMailSender;
////
////    @BeforeEach
////    void setUp() {
////        MockitoAnnotations.openMocks(this);
////    }
////
//////    @Disabled("Disabled for CI/CD - mocked test, no real email sent")
////    @Test
////    void testSendMail() {
////
////        Mockito.doNothing().when(javaMailSender).send(any(SimpleMailMessage.class));
////
////        emailService.sendEmail(
////                "ashujhaaaa@gmail.com",
////                "Return to sentiment analysis",
////                "This is a test email to verify the email service functionality."
////        );
////
////        Mockito.verify(javaMailSender, Mockito.times(1))
////                .send(any(SimpleMailMessage.class));
////    }
////
////
////}
//
//
//package net.engineeringdigest.journalApp.service;
//
//import net.engineeringdigest.journalApp.config.TestMailConfig;
//import net.engineeringdigest.journalApp.services.EmailService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//import org.springframework.context.annotation.Import;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.util.ReflectionTestUtils;
//
//import static org.junit.jupiter.api.Assertions.assertTrue;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.when;
//
//@ActiveProfiles("dev")
//@Import(TestMailConfig.class)
//public class EmailServiceTests {
//
//    @InjectMocks
//    private EmailService emailService;
//
//    @BeforeEach
//    void setUp() {
//        MockitoAnnotations.openMocks(this);
//
//        // Set test values for @Value fields
//        ReflectionTestUtils.setField(emailService, "resendApiKey", "test-api-key");
//        ReflectionTestUtils.setField(emailService, "fromEmail", "test@example.com");
//    }
//
//    @Test
//    void testSendEmail() {
//        // This will test the actual HTTP implementation
//        // In a real test environment, you'd mock the HTTP client
//
//        boolean result = emailService.sendEmail(
//                "ashujhaaaa@gmail.com",
//                "Test Subject",
//                "Test body content"
//        );
//
//        // In a mocked environment, this would return true
//        // In integration tests, this would make actual API calls
//    }
//
//    @Test
//    void testSendHtmlEmail() {
//        boolean result = emailService.sendHtmlEmail(
//                "test@example.com",
//                "Test HTML Subject",
//                "<h1>Test HTML Content</h1>"
//        );
//
//        // Test HTML email functionality
//    }
//}


package net.engineeringdigest.journalApp.service;

import net.engineeringdigest.journalApp.config.TestMailConfig;
import net.engineeringdigest.journalApp.services.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("dev")
@Import(TestMailConfig.class)
public class EmailServiceTests {

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Set test values for @Value fields (updated for SendGrid)
        ReflectionTestUtils.setField(emailService, "sendGridApiKey", "SG.test-api-key");
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@example.com");
        ReflectionTestUtils.setField(emailService, "fromName", "Test App");
    }

    @Test
    void testSendEmail() {
        // Test plain text email
        boolean result = emailService.sendEmail(
                "test@example.com",
                "Test Subject",
                "Test body content"
        );

        // In a mocked environment, this would return true
        // In integration tests, this would make actual API calls
    }

    @Test
    void testSendHtmlEmail() {
        // Test HTML email
        boolean result = emailService.sendHtmlEmail(
                "test@example.com",
                "Test HTML Subject",
                "<h1>Test HTML Content</h1><p>This is a test email.</p>"
        );

        // Test HTML email functionality
    }
}