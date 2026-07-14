package net.engineeringdigest.journalApp.service;

import net.engineeringdigest.journalApp.services.EmailService;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class EmailServiceTests {

    private EmailService emailService;
    private Call mockCall;

    @BeforeEach
    void setUp() throws Exception {
        OkHttpClient mockClient = Mockito.mock(OkHttpClient.class);
        mockCall = Mockito.mock(Call.class);

        when(mockClient.newCall(any())).thenReturn(mockCall);

        emailService = new EmailService(mockClient);
        setField(emailService, "brevoApiKey", "xkeysib-test-key");
        setField(emailService, "fromEmail", "test@example.com");
        setField(emailService, "fromName", "Test App");
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void sendEmail_shouldReturnTrue_whenBrevoAcceptsRequest() throws Exception {
        when(mockCall.execute()).thenReturn(response(201, "Created"));

        boolean result = emailService.sendEmail(
                "receiver@example.com",
                "Test Subject",
                "Body"
        );

        assertTrue(result);
    }

    @Test
    void sendEmail_shouldReturnFalse_whenBrevoRejectsRequest() throws Exception {
        when(mockCall.execute()).thenReturn(response(401, "Unauthorized"));

        boolean result = emailService.sendEmail(
                "receiver@example.com",
                "Test Subject",
                "Body"
        );

        assertFalse(result);
    }

    @Test
    void sendEmail_shouldReturnFalse_whenHttpThrowsException() throws Exception {
        when(mockCall.execute()).thenThrow(new IOException("Simulated HTTP failure"));

        boolean result = emailService.sendEmail(
                "receiver@example.com",
                "Test Subject",
                "Body"
        );

        assertFalse(result);
    }

    private Response response(int code, String message) {
        Request request = new Request.Builder()
                .url("https://api.brevo.com/v3/smtp/email")
                .build();

        return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message(message)
                .body(ResponseBody.create(
                        "{\"message\":\"" + message + "\"}",
                        MediaType.parse("application/json")
                ))
                .build();
    }
}
