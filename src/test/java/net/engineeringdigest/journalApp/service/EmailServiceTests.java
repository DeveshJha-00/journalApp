package net.engineeringdigest.journalApp.service;

import net.engineeringdigest.journalApp.services.EmailService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class EmailServiceTests {

    @Autowired
    private EmailService emailService;

    @Disabled
    @Test
    void testSendMail(){
        emailService.sendEmail("cafog63099@lewou.com","Testing Email Service",
                "This is a test email to verify the email service functionality.");
    }

}
