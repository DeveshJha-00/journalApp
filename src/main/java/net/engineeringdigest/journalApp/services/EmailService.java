package net.engineeringdigest.journalApp.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender javaMailSender;

    public boolean sendEmail(String receiver, String subject, String body) {
        try{
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(receiver);
            mail.setSubject(subject);
            mail.setText(body);
            javaMailSender.send(mail);
            log.info("Plain text email sent successfully to: {}", receiver);
            return true;
        }catch (Exception e){
            log.error("Error sending plain text email to {}: {}", receiver, e.getMessage());
            return false;
        }
    }


    public boolean sendHtmlEmail(String receiver, String subject, String htmlBody) {
        try {
            log.info("📧 Preparing HTML email for: {} with subject: '{}'", receiver, subject);

            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(receiver);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true indicates HTML content

            log.info("📤 Sending HTML email to: {} via SMTP...", receiver);
            javaMailSender.send(mimeMessage);
            log.info("✅ HTML email sent successfully to: {}", receiver);
            return true;
        } catch (MessagingException e) {
            log.error("❌ MessagingException sending HTML email to {}: {}", receiver, e.getMessage());
            log.error("📋 Full exception details: ", e);

            // Fallback to plain text email
            try {
                log.info("🔄 Attempting fallback plain text email to: {}", receiver);
                String plainTextBody = htmlBody.replaceAll("<[^>]*>", ""); // Strip HTML tags
                boolean fallbackSuccess = sendEmail(receiver, subject, plainTextBody);
                if (fallbackSuccess) {
                    log.info("✅ Sent fallback plain text email to: {}", receiver);
                    return true;
                } else {
                    log.error("❌ Failed to send fallback email to: {}", receiver);
                    return false;
                }
            } catch (Exception fallbackError) {
                log.error("❌ Failed to send fallback email to {}: {}", receiver, fallbackError.getMessage());
                return false;
            }
        } catch (Exception e) {
            log.error("❌ Unexpected error sending HTML email to {}: {}", receiver, e.getMessage());
            log.error("📋 Full exception details: ", e);
            return false;
        }
    }

}
