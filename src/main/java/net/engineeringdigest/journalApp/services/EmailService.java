package net.engineeringdigest.journalApp.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
@Slf4j
public class EmailService {

    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${brevo.api.key}")
    private String brevoApiKey;

    @Value("${brevo.from.email}")
    private String fromEmail;

    @Value("${brevo.from.name:JournalApp}")
    private String fromName;

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

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
                String plainTextBody = htmlBody.replaceAll("<[^>]*>", "");
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
            log.info("Brevo Key: '{}'", brevoApiKey);
            log.info("Key length: {}", brevoApiKey != null ? brevoApiKey.length() : 0);

            // Debug logs
            log.info("🔍 Debug - API Key present: {}", brevoApiKey != null && !brevoApiKey.isEmpty());
            log.info("🔍 Debug - From Email: '{}'", fromEmail);
            log.info("🔍 Debug - From Name: '{}'", fromName);

            // Build Brevo payload
            Map<String, Object> payload = buildBrevoPayload(receiver, subject, content, isHtml);
            String jsonPayload = objectMapper.writeValueAsString(payload);

            log.info("🔍 Debug - JSON Payload: {}", jsonPayload);

            RequestBody body = RequestBody.create(
                    jsonPayload,
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(BREVO_API_URL)
                    .post(body)
                    .addHeader("api-key", brevoApiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            log.info("📤 Sending {} email to: {} via Brevo API...",
                    isHtml ? "HTML" : "plain text", receiver);

            try (Response response = httpClient.newCall(request).execute()) {

                String responseBody =
                        response.body() != null ? response.body().string() : "null";

                log.info("🔍 Debug - Response Status: {}", response.code());
                log.info("🔍 Debug - Response Headers: {}", response.headers());
                log.info("🔍 Debug - Response Body: {}", responseBody);

                if (response.isSuccessful()) {
                    log.info("✅ Brevo API responded with success for {}", receiver);
                    return true;
                } else {
                    log.error("❌ Brevo API failed with status {}: {}", response.code(), responseBody);
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

    private Map<String, Object> buildBrevoPayload(String receiver, String subject, String content, boolean isHtml) {

        Map<String, Object> payload = new HashMap<>();

        // Sender
        Map<String, String> sender = new HashMap<>();
        sender.put("email", fromEmail);
        sender.put("name", fromName);
        payload.put("sender", sender);

        // Recipient list
        Map<String, String> to = new HashMap<>();
        to.put("email", receiver);
        payload.put("to", Collections.singletonList(to));

        // Subject
        payload.put("subject", subject);

        // Content
        if (isHtml) {
            payload.put("htmlContent", content);
        } else {
            payload.put("textContent", content);
        }

        return payload;
    }
}