package net.engineeringdigest.journalApp.service;

import net.engineeringdigest.journalApp.services.EmailService;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class EmailServiceTests {

    private EmailService emailService;

    @BeforeEach
    void setUp() throws Exception {
        emailService = new EmailService();

        // Inject fake Brevo config
        setField(emailService, "brevoApiKey", "xkeysib-test-key");
        setField(emailService, "fromEmail", "test@example.com");
        setField(emailService, "fromName", "Test App");

        // ---- Proper HTTP mocking without mocking final Response ----
        OkHttpClient mockClient = Mockito.mock(OkHttpClient.class);
        Call mockCall = Mockito.mock(Call.class);

        when(mockClient.newCall(any())).thenReturn(mockCall);
        when(mockCall.execute()).thenThrow(new IOException("Simulated HTTP failure"));

        setField(emailService, "httpClient", mockClient);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void sendEmail_shouldReturnFalse_whenHttpThrowsException() {
        boolean result = emailService.sendEmail(
                "test@example.com",
                "Test Subject",
                "Body"
        );

        assertFalse(result);
    }
}