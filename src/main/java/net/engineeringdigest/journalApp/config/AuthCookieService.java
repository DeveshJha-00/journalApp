package net.engineeringdigest.journalApp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;

@Component
public class AuthCookieService {

    public static final String JWT_COOKIE_NAME = "journal_jwt";

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    public void addJwtCookie(HttpServletResponse response, String jwt) {
        response.addHeader("Set-Cookie", buildCookie(jwt, 60 * 60));
    }

    public void clearJwtCookie(HttpServletResponse response) {
        response.addHeader("Set-Cookie", buildCookie("", 0));
    }

    private String buildCookie(String value, int maxAgeSeconds) {
        boolean secure = frontendUrl != null && frontendUrl.startsWith("https://");
        String sameSite = secure ? "None" : "Lax";

        StringBuilder cookie = new StringBuilder();
        cookie.append(JWT_COOKIE_NAME).append("=").append(value == null ? "" : value)
                .append("; Path=/")
                .append("; Max-Age=").append(maxAgeSeconds)
                .append("; HttpOnly")
                .append("; SameSite=").append(sameSite);

        if (secure) {
            cookie.append("; Secure");
        }

        return cookie.toString();
    }
}
