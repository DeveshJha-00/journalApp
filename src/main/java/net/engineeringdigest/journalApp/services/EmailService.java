package net.engineeringdigest.journalApp.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class EmailService {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${brevo.api.key}")
    private String brevoApiKey;

    @Value("${brevo.from.email}")
    private String fromEmail;

    @Value("${brevo.from.name:JournalApp}")
    private String fromName;

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    public EmailService() {
        this(new OkHttpClient());
    }

    public EmailService(OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Send a plain text email.
     */
    public boolean sendEmail(String receiver, String subject, String body) {
        return sendEmailInternal(receiver, subject, body, false);
    }

    /**
     * Send an HTML email, with fallback to plain text if needed.
     */
    public boolean sendHtmlEmail(String receiver, String subject, String htmlBody) {
        try {
            log.info("Preparing HTML email for {} with subject '{}'", receiver, subject);

            boolean sent = sendEmailInternal(receiver, subject, htmlBody, true);

            if (sent) {
                log.info("HTML email sent successfully to {}", receiver);
                return true;
            }

            log.error("Failed to send HTML email to {}", receiver);
            log.info("Attempting fallback plain text email to {}", receiver);
            String plainTextBody = htmlBody.replaceAll("<[^>]*>", "");
            boolean fallbackSuccess = sendEmail(receiver, subject, plainTextBody);

            if (fallbackSuccess) {
                log.info("Sent fallback plain text email to {}", receiver);
                return true;
            }

            log.error("Fallback email also failed for {}", receiver);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error sending HTML email to {}: {}", receiver, e.getMessage());
            return false;
        }
    }

    private boolean sendEmailInternal(String receiver, String subject, String content, boolean isHtml) {
        try {
            if (brevoApiKey == null || brevoApiKey.isBlank()) {
                log.error("Brevo API key is not configured; cannot send email to {}", receiver);
                return false;
            }
            if (fromEmail == null || fromEmail.isBlank()) {
                log.error("Brevo sender email is not configured; cannot send email to {}", receiver);
                return false;
            }

            log.info("Sending {} email to {} via Brevo API",
                    isHtml ? "HTML" : "plain text", receiver);

            Map<String, Object> payload = buildBrevoPayload(receiver, subject, content, isHtml);
            String jsonPayload = objectMapper.writeValueAsString(payload);

            RequestBody body = RequestBody.create(
                    jsonPayload,
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(BREVO_API_URL)
                    .post(body)
                    // Never log this header or jsonPayload; both may contain secrets/user content.
                    .addHeader("api-key", brevoApiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            Call call = httpClient.newCall(request);
            try (Response response = call.execute()) {
                if (response.body() != null) {
                    response.body().close();
                }

                if (response.isSuccessful()) {
                    log.info("Brevo API accepted {} email for {} with status {}",
                            isHtml ? "HTML" : "plain text", receiver, response.code());
                    return true;
                }

                log.error("Brevo API failed to send {} email to {} with status {}",
                        isHtml ? "HTML" : "plain text", receiver, response.code());
                return false;
            }
        } catch (IOException e) {
            log.error("IO error sending email to {}: {}", receiver, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error sending email to {}: {}", receiver, e.getMessage());
            return false;
        }
    }

    private Map<String, Object> buildBrevoPayload(String receiver, String subject, String content, boolean isHtml) {
        Map<String, Object> payload = new HashMap<>();

        Map<String, String> sender = new HashMap<>();
        sender.put("email", fromEmail);
        sender.put("name", fromName);
        payload.put("sender", sender);

        Map<String, String> to = new HashMap<>();
        to.put("email", receiver);
        payload.put("to", Collections.singletonList(to));

        payload.put("subject", subject);

        if (isHtml) {
            payload.put("htmlContent", content);
        } else {
            payload.put("textContent", content);
        }

        return payload;
    }
}
