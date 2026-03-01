package net.engineeringdigest.journalApp.config;

import lombok.extern.slf4j.Slf4j;
import net.engineeringdigest.journalApp.entity.User;
import net.engineeringdigest.journalApp.repository.UserRepository;
import net.engineeringdigest.journalApp.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

@Component
@Slf4j
public class OAuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        if (email == null || email.isBlank()) {
            log.error("OAuth2 login failed: no email attribute from provider");
            getRedirectStrategy().sendRedirect(request, response,
                    frontendUrl + "/auth/callback?error=no_email");
            return;
        }

        try {
            // Look up user by email
            User user = userRepository.findFirstByEmail(email);

            if (user == null) {
                // Create new user for Google OAuth
                user = User.builder()
                        .userName(generateUsername(email))
                        .email(email)
                        .password(null) // No password for OAuth users
                        .authProvider("GOOGLE")
                        .sentimentAnalysis(true) // Enable by default for new OAuth users
                        .roles(Arrays.asList("USER"))
                        .journalEntryList(new ArrayList<>())
                        .build();
                userRepository.save(user);
                log.info("Created new OAuth user: {} (email: {})", user.getUserName(), email);
            } else {
                log.info("Existing user logged in via OAuth: {} (email: {})", user.getUserName(), email);
            }

            // Generate JWT token
            String jwt = jwtUtil.generateToken(user.getUserName());

            // Redirect to frontend with token
            String redirectUrl = frontendUrl + "/auth/callback?token=" + jwt;
            log.info("OAuth login successful, redirecting to frontend for user: {}", user.getUserName());
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);

        } catch (Exception e) {
            log.error("Error during OAuth authentication for email {}: {}", email, e.getMessage());
            getRedirectStrategy().sendRedirect(request, response,
                    frontendUrl + "/auth/callback?error=auth_failed");
        }
    }

    /**
     * Generate a unique username from the email address.
     * Uses the part before @ and appends a short random suffix to avoid collisions.
     */
    private String generateUsername(String email) {
        String base = email.split("@")[0]
                .replaceAll("[^a-zA-Z0-9]", "")
                .toLowerCase();
        if (base.length() > 20) {
            base = base.substring(0, 20);
        }
        // Add random suffix to ensure uniqueness
        String suffix = String.valueOf((int) (Math.random() * 9000) + 1000);
        return base + suffix;
    }
}
