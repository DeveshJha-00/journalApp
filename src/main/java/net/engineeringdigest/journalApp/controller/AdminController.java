package net.engineeringdigest.journalApp.controller;

import net.engineeringdigest.journalApp.entity.User;
import net.engineeringdigest.journalApp.services.BiweeklyReportService;
import net.engineeringdigest.journalApp.services.EmailService;
import net.engineeringdigest.journalApp.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private BiweeklyReportService biweeklyReportService;

    @Autowired
    private EmailService emailService;

    @GetMapping("/all-users")
    public ResponseEntity<?> getAllUsers() {
        List<User> allUsers = userService.findAllUsers();
        if (allUsers != null && !allUsers.isEmpty()) {
            return new ResponseEntity<>(allUsers, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PostMapping("add-admin")
    public void createAdmin(@RequestBody User user){
        userService.saveAdmin(user);
    }

    /**
     * Test endpoint to send a single report for a specific user
     */
    @PostMapping("/test-report/{username}")
    public ResponseEntity<String> testSingleReport(@PathVariable String username) {
        try {
            User user = userService.findByUsername(username);
            if (user == null) {
                return ResponseEntity.badRequest().body("User not found: " + username);
            }

            if (!user.isSentimentAnalysis()) {
                return ResponseEntity.badRequest().body("User " + username + " does not have sentiment analysis enabled");
            }

            // Clear cache first to force fresh report generation
            biweeklyReportService.clearReportCache(user.getId().toHexString());

            boolean success = biweeklyReportService.generateAndSendReport(user);
            if (success) {
                return ResponseEntity.ok("✅ Report generated and sent successfully for user: " + username);
            } else {
                return ResponseEntity.status(500).body("❌ Failed to generate/send report for user: " + username);
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /**
     * Test endpoint to send a simple test email
     */
    @PostMapping("/test-email")
    public ResponseEntity<String> testEmail(@RequestParam String email, @RequestParam(defaultValue = "Test Subject") String subject) {
        try {
            String testContent = "<h1>Test Email</h1><p>This is a test email from your Journal App!</p><p>If you receive this, your email configuration is working correctly.</p>";
            boolean success = emailService.sendHtmlEmail(email, subject, testContent);

            if (success) {
                return ResponseEntity.ok("✅ Test email sent successfully to: " + email);
            } else {
                return ResponseEntity.status(500).body("❌ Failed to send test email to: " + email);
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error sending test email: " + e.getMessage());
        }
    }

    /**
     * Clear report cache for a specific user
     */
    @DeleteMapping("/clear-cache/{username}")
    public ResponseEntity<String> clearUserCache(@PathVariable String username) {
        try {
            User user = userService.findByUsername(username);
            if (user == null) {
                return ResponseEntity.badRequest().body("User not found: " + username);
            }

            biweeklyReportService.clearReportCache(user.getId().toHexString());
            return ResponseEntity.ok("🗑️ Cache cleared for user: " + username);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error clearing cache: " + e.getMessage());
        }
    }

}
