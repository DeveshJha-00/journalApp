////package net.engineeringdigest.journalApp.services;
////
////import lombok.extern.slf4j.Slf4j;
////import org.springframework.beans.factory.annotation.Autowired;
////import org.springframework.mail.SimpleMailMessage;
////import org.springframework.mail.javamail.JavaMailSender;
////import org.springframework.mail.javamail.MimeMessageHelper;
////import org.springframework.stereotype.Service;
////
////import javax.mail.MessagingException;
////import javax.mail.internet.MimeMessage;
////
////@Service
////@Slf4j
////public class EmailService {
////
////    @Autowired
////    private JavaMailSender javaMailSender;
////
////    public boolean sendEmail(String receiver, String subject, String body) {
////        try{
////            SimpleMailMessage mail = new SimpleMailMessage();
////            mail.setTo(receiver);
////            mail.setSubject(subject);
////            mail.setText(body);
////            javaMailSender.send(mail);
////            log.info("Plain text email sent successfully to: {}", receiver);
////            return true;
////        }catch (Exception e){
////            log.error("Error sending plain text email to {}: {}", receiver, e.getMessage());
////            return false;
////        }
////    }
////
////
////    public boolean sendHtmlEmail(String receiver, String subject, String htmlBody) {
////        try {
////            log.info("📧 Preparing HTML email for: {} with subject: '{}'", receiver, subject);
////
////            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
////            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
////
////            helper.setTo(receiver);
////            helper.setSubject(subject);
////            helper.setText(htmlBody, true); // true indicates HTML content
////
////            log.info("📤 Sending HTML email to: {} via SMTP...", receiver);
////            javaMailSender.send(mimeMessage);
////            log.info("✅ HTML email sent successfully to: {}", receiver);
////            return true;
////        } catch (MessagingException e) {
////            log.error("❌ MessagingException sending HTML email to {}: {}", receiver, e.getMessage());
////            log.error("📋 Full exception details: ", e);
////
////            // Fallback to plain text email
////            try {
////                log.info("🔄 Attempting fallback plain text email to: {}", receiver);
////                String plainTextBody = htmlBody.replaceAll("<[^>]*>", ""); // Strip HTML tags
////                boolean fallbackSuccess = sendEmail(receiver, subject, plainTextBody);
////                if (fallbackSuccess) {
////                    log.info("✅ Sent fallback plain text email to: {}", receiver);
////                    return true;
////                } else {
////                    log.error("❌ Failed to send fallback email to: {}", receiver);
////                    return false;
////                }
////            } catch (Exception fallbackError) {
////                log.error("❌ Failed to send fallback email to {}: {}", receiver, fallbackError.getMessage());
////                return false;
////            }
////        } catch (Exception e) {
////            log.error("❌ Unexpected error sending HTML email to {}: {}", receiver, e.getMessage());
////            log.error("📋 Full exception details: ", e);
////            return false;
////        }
////    }
////
////}
//
//
//package net.engineeringdigest.journalApp.services;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.extern.slf4j.Slf4j;
//import okhttp3.*;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.Map;
//
//@Service
//@Slf4j
//public class EmailService {
//
//    private final OkHttpClient httpClient = new OkHttpClient();
//    private final ObjectMapper objectMapper = new ObjectMapper();
//
//    @Value("${resend.api.key}")
//    private String resendApiKey;
//
//    @Value("${resend.from.email}")
//    private String fromEmail;
//
//    private static final String RESEND_API_URL = "https://api.resend.com/emails";
//
//    /**
//     * Send a plain text email
//     */
//    public boolean sendEmail(String receiver, String subject, String body) {
//        return sendEmailInternal(receiver, subject, body, false);
//    }
//
//    /**
//     * Send an HTML email (with fallback to plain text if needed)
//     */
//    public boolean sendHtmlEmail(String receiver, String subject, String htmlBody) {
//        try {
//            log.info("📧 Preparing HTML email for: {} with subject: '{}'", receiver, subject);
//            boolean sent = sendEmailInternal(receiver, subject, htmlBody, true);
//
//            if (sent) {
//                log.info("✅ HTML email sent successfully to: {}", receiver);
//                return true;
//            } else {
//                log.error("❌ Failed to send HTML email to {}", receiver);
//                // Fallback to plain text
//                log.info("🔄 Attempting fallback plain text email to: {}", receiver);
//                String plainTextBody = htmlBody.replaceAll("<[^>]*>", ""); // strip HTML tags
//                boolean fallbackSuccess = sendEmail(receiver, subject, plainTextBody);
//                if (fallbackSuccess) {
//                    log.info("✅ Sent fallback plain text email to: {}", receiver);
//                    return true;
//                } else {
//                    log.error("❌ Fallback email also failed for {}", receiver);
//                    return false;
//                }
//            }
//        } catch (Exception e) {
//            log.error("❌ Unexpected error sending HTML email to {}: {}", receiver, e.getMessage(), e);
//            return false;
//        }
//    }
//
//    private boolean sendEmailInternal(String receiver, String subject, String content, boolean isHtml) {
//        try {
//            // Log environment variables
//            log.info("🔍 Debug - API Key present: {}", resendApiKey != null && !resendApiKey.isEmpty());
//            log.info("🔍 Debug - From Email: '{}'", fromEmail);
//
//            Map<String, Object> payload = new HashMap<>();
//            payload.put("from", fromEmail);
//            payload.put("to", new String[]{receiver});
//            payload.put("subject", subject);
//            if (isHtml) {
//                payload.put("html", content);
//            } else {
//                payload.put("text", content);
//            }
//
//            String jsonPayload = objectMapper.writeValueAsString(payload);
//
//            // Log the exact JSON payload
//            log.info("🔍 Debug - JSON Payload: {}", jsonPayload);
//
//            RequestBody body = RequestBody.create(
//                    jsonPayload,
//                    MediaType.parse("application/json")
//            );
//
//            Request request = new Request.Builder()
//                    .url(RESEND_API_URL)
//                    .post(body)
//                    .addHeader("Authorization", "Bearer " + resendApiKey)
//                    .addHeader("Content-Type", "application/json")
//                    .build();
//
//            log.info("📤 Sending {} email to: {} via Resend API...", isHtml ? "HTML" : "plain text", receiver);
//
//            try (Response response = httpClient.newCall(request).execute()) {
//                String responseBody = response.body() != null ? response.body().string() : "null";
//
//                // Log detailed response information
//                log.info("🔍 Debug - Response Status: {}", response.code());
//                log.info("🔍 Debug - Response Headers: {}", response.headers());
//                log.info("🔍 Debug - Response Body: {}", responseBody);
//
//                if (response.isSuccessful()) {
//                    log.info("✅ Resend API responded with success for {}", receiver);
//                    return true;
//                } else {
//                    log.error("❌ Resend API failed with status {}: {}", response.code(), responseBody);
//                    return false;
//                }
//            }
//        } catch (IOException e) {
//            log.error("❌ IO error sending email to {}: {}", receiver, e.getMessage(), e);
//            return false;
//        } catch (Exception e) {
//            log.error("❌ Unexpected error sending email to {}: {}", receiver, e.getMessage(), e);
//            return false;
//        }
//    }
//}


package net.engineeringdigest.journalApp.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EmailService {

    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${sendgrid.api.key}")
    private String sendGridApiKey;

    @Value("${sendgrid.from.email}")
    private String fromEmail;

    @Value("${sendgrid.from.name:JournalApp}")
    private String fromName;

    private static final String SENDGRID_API_URL = "https://api.sendgrid.com/v3/mail/send";

    /**
     * Send a plain text email
     */
    public boolean sendEmail(String receiver, String subject, String body) {
        return sendEmailInternal(receiver, subject, body, false);
    }

    /**
     * Send an HTML email (with fallback to plain text if needed)
     */
    public boolean sendHtmlEmail(String receiver, String subject, String htmlBody) {
        try {
            log.info("📧 Preparing HTML email for: {} with subject: '{}'", receiver, subject);
            boolean sent = sendEmailInternal(receiver, subject, htmlBody, true);

            if (sent) {
                log.info("✅ HTML email sent successfully to: {}", receiver);
                return true;
            } else {
                log.error("❌ Failed to send HTML email to {}", receiver);
                // Fallback to plain text
                log.info("🔄 Attempting fallback plain text email to: {}", receiver);
                String plainTextBody = htmlBody.replaceAll("<[^>]*>", ""); // strip HTML tags
                boolean fallbackSuccess = sendEmail(receiver, subject, plainTextBody);
                if (fallbackSuccess) {
                    log.info("✅ Sent fallback plain text email to: {}", receiver);
                    return true;
                } else {
                    log.error("❌ Fallback email also failed for {}", receiver);
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("❌ Unexpected error sending HTML email to {}: {}", receiver, e.getMessage(), e);
            return false;
        }
    }

    private boolean sendEmailInternal(String receiver, String subject, String content, boolean isHtml) {
        try {
            // Log environment variables for debugging
            log.info("🔍 Debug - API Key present: {}", sendGridApiKey != null && !sendGridApiKey.isEmpty());
            log.info("🔍 Debug - From Email: '{}'", fromEmail);
            log.info("🔍 Debug - From Name: '{}'", fromName);

            // Build SendGrid payload
            Map<String, Object> payload = buildSendGridPayload(receiver, subject, content, isHtml);
            String jsonPayload = objectMapper.writeValueAsString(payload);

            // Log the exact JSON payload for debugging
            log.info("🔍 Debug - JSON Payload: {}", jsonPayload);

            RequestBody body = RequestBody.create(
                    jsonPayload,
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(SENDGRID_API_URL)
                    .post(body)
                    .addHeader("Authorization", "Bearer " + sendGridApiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            log.info("📤 Sending {} email to: {} via SendGrid API...", isHtml ? "HTML" : "plain text", receiver);

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "null";

                // Log detailed response information
                log.info("🔍 Debug - Response Status: {}", response.code());
                log.info("🔍 Debug - Response Headers: {}", response.headers());
                log.info("🔍 Debug - Response Body: {}", responseBody);

                if (response.isSuccessful()) {
                    log.info("✅ SendGrid API responded with success for {}", receiver);
                    return true;
                } else {
                    log.error("❌ SendGrid API failed with status {}: {}", response.code(), responseBody);
                    return false;
                }
            }
        } catch (IOException e) {
            log.error("❌ IO error sending email to {}: {}", receiver, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("❌ Unexpected error sending email to {}: {}", receiver, e.getMessage(), e);
            return false;
        }
    }

    private Map<String, Object> buildSendGridPayload(String receiver, String subject, String content, boolean isHtml) {
        Map<String, Object> payload = new HashMap<>();

        // From address
        Map<String, String> from = new HashMap<>();
        from.put("email", fromEmail);
        from.put("name", fromName);
        payload.put("from", from);

        // Personalizations (recipients and subject)
        Map<String, Object> personalization = new HashMap<>();

        Map<String, String> to = new HashMap<>();
        to.put("email", receiver);
        personalization.put("to", Arrays.asList(to));
        personalization.put("subject", subject);

        payload.put("personalizations", Arrays.asList(personalization));

        // Content
        Map<String, String> contentMap = new HashMap<>();
        if (isHtml) {
            contentMap.put("type", "text/html");
            contentMap.put("value", content);
        } else {
            contentMap.put("type", "text/plain");
            contentMap.put("value", content);
        }
        payload.put("content", Arrays.asList(contentMap));

        return payload;
    }
}
