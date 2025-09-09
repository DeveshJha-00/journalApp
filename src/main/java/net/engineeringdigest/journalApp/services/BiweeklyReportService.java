package net.engineeringdigest.journalApp.services;

import lombok.extern.slf4j.Slf4j;
import net.engineeringdigest.journalApp.entity.JournalEntry;
import net.engineeringdigest.journalApp.entity.User;
import net.engineeringdigest.journalApp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BiweeklyReportService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private RedisService redisService;

    @Value("${report.frequency.days:14}")
    private int reportFrequencyDays;

    @Value("${report.min.entries:1}")
    private int minEntriesForReport;

    @Value("${report.max.entries:50}")
    private int maxEntriesPerReport;

    @Value("${report.email.subject:Your Bi-weekly Mental Health Insights}")
    private String reportEmailSubject;


    @Scheduled(cron = "0 0 9 */14 * ?")
//    @Scheduled(cron = "0 */2 * * * ?")  // Every 2 minutes
    public void generateAndSendBiweeklyReports() {
        log.info("Starting bi-weekly report generation process");
        
        try {
            List<User> eligibleUsers = getEligibleUsers();
            log.info("Found {} eligible users for bi-weekly reports", eligibleUsers.size());

            int successCount = 0;
            int failureCount = 0;

            // Process users in batches to avoid overwhelming the LLM API
            List<List<User>> batches = createBatches(eligibleUsers, 10);
            
            for (List<User> batch : batches) {
                for (User user : batch) {
                    try {
                        if (generateAndSendReport(user)) {
                            successCount++;
                        } else {
                            failureCount++;
                        }
                        
                        // Small delay between users to respect API rate limits
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        log.error("Failed to generate report for user {}: {}", user.getUserName(), e.getMessage());
                        failureCount++;
                    }
                }
                
                // Longer delay between batches
                if (batches.indexOf(batch) < batches.size() - 1) {
                    Thread.sleep(5000);
                }
            }

            log.info("Bi-weekly report generation completed. Success: {}, Failures: {}", successCount, failureCount);
        } catch (Exception e) {
            log.error("Error in bi-weekly report generation process: {}", e.getMessage());
        }
    }

    /**
     * Generate and send report for a specific user
     */
    public boolean generateAndSendReport(User user) {
        try {
            log.info("🔄 Starting report generation for user: {} (email: {})", user.getUserName(), user.getEmail());

            // Get journal entries from the last 14 days with sentiment data
            List<JournalEntry> recentEntries = getRecentEntriesWithSentiment(user, reportFrequencyDays);
            log.info("📊 Found {} recent entries for user: {}", recentEntries.size(), user.getUserName());

            if (recentEntries.size() < minEntriesForReport) {
                log.warn("❌ User {} has insufficient entries ({}) for report generation (minimum required: {})",
                         user.getUserName(), recentEntries.size(), minEntriesForReport);
                return false;
            }

            String reportCacheKey = buildReportCacheKey(user.getId().toHexString());

            // PHASE 1: Report Content Acquisition (Cached vs Fresh Generation)
            String reportContent;
            LocalDateTime lastReportTime = getLastReportGenerationTime(reportCacheKey);
            List<JournalEntry> newEntries = getEntriesAfterTimestamp(user, lastReportTime);

            log.info("📈 Entry analysis for user {}: {} total entries, {} new entries since last report",
                     user.getUserName(), recentEntries.size(), newEntries.size());

            if (newEntries.isEmpty() && isReportRecentlyGenerated(reportCacheKey)) {
                // No new entries - use cached report content
                reportContent = getCachedReportContent(reportCacheKey);
                if (reportContent != null) {
                    log.info("📋 Using cached report content for user: {} (no new entries)", user.getUserName());
                } else {
                    log.warn("⚠️ Cache indicated recent report but content not found, generating fresh report for user: {}", user.getUserName());
                    reportContent = generateFreshReport(recentEntries, user.getUserName(), reportCacheKey);
                }
            } else {
                // New entries exist or no cached report - generate fresh report
                log.info("🆕 Generating fresh report for user: {} ({} new entries)", user.getUserName(), newEntries.size());
                reportContent = generateFreshReport(recentEntries, user.getUserName(), reportCacheKey);
            }

            // Validate report content was obtained
            if (reportContent == null || reportContent.trim().isEmpty()) {
                log.error("❌ No report content available for user: {}", user.getUserName());
                return false;
            }

            // PHASE 2: Email Delivery (Always Execute for Eligible Users)
            log.info("📧 Creating HTML email template for user: {} (content length: {} chars)",
                     user.getUserName(), reportContent.length());
            String htmlContent = createEmailTemplate(user.getUserName(), reportContent);

            log.info("📤 Attempting to send email to: {} for user: {}", user.getEmail(), user.getUserName());
            boolean emailSent = emailService.sendHtmlEmail(user.getEmail(), reportEmailSubject, htmlContent);

            if (emailSent) {
                log.info("✅ Successfully sent bi-weekly report email for user: {}", user.getUserName());
                return true;
            } else {
                log.error("❌ Failed to send bi-weekly report email for user: {}", user.getUserName());
                return false;
            }

        } catch (Exception e) {
            log.error("❌ Failed to generate report for user {}: {}", user.getUserName(), e.getMessage());
            log.error("📋 Full exception details: ", e);

            // Send fallback email
            try {
                log.info("🔄 Attempting fallback email for user: {}", user.getUserName());
                String fallbackContent = createFallbackEmailTemplate(user.getUserName());
                boolean fallbackSent = emailService.sendHtmlEmail(user.getEmail(), reportEmailSubject, fallbackContent);
                if (fallbackSent) {
                    log.info("✅ Sent fallback email to user: {}", user.getUserName());
                } else {
                    log.error("❌ Failed to send fallback email to user: {}", user.getUserName());
                }
            } catch (Exception emailError) {
                log.error("❌ Failed to send fallback email to user {}: {}", user.getUserName(), emailError.getMessage());
            }

            return false;
        }
    }

    /**
     * Clear report cache for a specific user (for testing purposes)
     */
    public void clearReportCache(String userId) {
        String reportCacheKey = buildReportCacheKey(userId);
        redisService.delete(reportCacheKey);
        log.info("🗑️ Cleared report cache for user ID: {}", userId);
    }

    /**
     * Clear all report caches (for testing purposes)
     */
    public void clearAllReportCaches() {
        // This would require implementing a pattern-based delete in RedisService
        log.info("🗑️ Clearing all report caches...");
        // For now, you can manually clear specific user caches
    }

    /**
     * Get cached report content for a user
     */
    private String getCachedReportContent(String cacheKey) {
        try {
            String cachedContent = redisService.get(cacheKey, String.class);
            if (cachedContent != null && !cachedContent.trim().isEmpty()) {
                log.debug("📋 Retrieved cached report content (length: {} chars)", cachedContent.length());
                return cachedContent;
            }
            return null;
        } catch (Exception e) {
            log.warn("⚠️ Failed to retrieve cached report content: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get the timestamp of the last report generation for a user
     */
    private LocalDateTime getLastReportGenerationTime(String cacheKey) {
        try {
            // Try to get timestamp from cache metadata
            String timestampKey = cacheKey + ":timestamp";
            String timestampStr = redisService.get(timestampKey, String.class);

            if (timestampStr != null) {
                return LocalDateTime.parse(timestampStr);
            }

            // Fallback: if no timestamp cached, return a time far in the past
            return LocalDateTime.now().minusDays(reportFrequencyDays + 1);
        } catch (Exception e) {
            log.debug("No cached timestamp found, using fallback time");
            return LocalDateTime.now().minusDays(reportFrequencyDays + 1);
        }
    }

    /**
     * Get journal entries created after a specific timestamp
     */
    private List<JournalEntry> getEntriesAfterTimestamp(User user, LocalDateTime timestamp) {
        if (timestamp == null) {
            return user.getJournalEntryList(); // Return all entries if no timestamp
        }

        return user.getJournalEntryList().stream()
                .filter(entry -> entry.getDate() != null && entry.getDate().isAfter(timestamp))
                .filter(entry -> entry.getSentimentAnalyzedAt() != null) // Only entries with sentiment analysis
                .collect(Collectors.toList());
    }

    /**
     * Generate fresh report content using Gemini API and cache it
     */
    private String generateFreshReport(List<JournalEntry> recentEntries, String userName, String reportCacheKey) {
        try {
            // Limit entries to avoid token limits
            List<JournalEntry> entriesToAnalyze = recentEntries.stream()
                    .limit(maxEntriesPerReport)
                    .collect(Collectors.toList());
            log.info("📝 Analyzing {} entries for fresh report generation", entriesToAnalyze.size());

            // Prepare data for LLM analysis
            List<Map<String, Object>> entriesData = prepareEntriesData(entriesToAnalyze);

            // Generate report using Gemini
            log.info("🤖 Calling Gemini API for fresh report generation");
            String reportContent = geminiService.generateBiweeklyReport(entriesData, userName);
            log.info("✅ Fresh report generated (length: {} chars)", reportContent.length());

            // Cache the new report with timestamp
            cacheReport(reportCacheKey, reportContent);
            cacheReportTimestamp(reportCacheKey);

            return reportContent;
        } catch (Exception e) {
            log.error("❌ Failed to generate fresh report: {}", e.getMessage());
            throw new RuntimeException("Failed to generate fresh report", e);
        }
    }

    private List<User> getEligibleUsers() {
        // Find users who have sentiment analysis enabled and valid email
        return userRepository.findAll().stream()
                .filter(user -> user.isSentimentAnalysis())
                .filter(user -> user.getEmail() != null && !user.getEmail().trim().isEmpty())
                .collect(Collectors.toList());
    }

    private List<JournalEntry> getRecentEntriesWithSentiment(User user, int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        
        return user.getJournalEntryList().stream()
                .filter(entry -> entry.getDate() != null && entry.getDate().isAfter(cutoffDate))
                .filter(entry -> entry.getSentimentAnalyzedAt() != null) // Only entries with sentiment data
                .sorted((e1, e2) -> e2.getDate().compareTo(e1.getDate())) // Most recent first
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> prepareEntriesData(List<JournalEntry> entries) {
        return entries.stream()
                .map(entry -> {
                    Map<String, Object> entryData = new HashMap<>();
                    entryData.put("date", entry.getDate());
                    entryData.put("title", entry.getTitle());
                    entryData.put("content", entry.getContent());
                    entryData.put("sentimentScore", entry.getSentimentScore());
                    entryData.put("sentimentLabel", entry.getSentimentLabel());
                    entryData.put("emotions", entry.getEmotions());
                    entryData.put("keywords", entry.getKeywords());
                    return entryData;
                })
                .collect(Collectors.toList());
    }

    private String createEmailTemplate(String userName, String reportContent) {
        return String.format(
            "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "    <title>Your Bi-weekly Mental Health Report</title>\n" +
            "    <style>\n" +
            "        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }\n" +
            "        .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }\n" +
            "        .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }\n" +
            "        .footer { text-align: center; margin-top: 30px; padding: 20px; background: #e9ecef; border-radius: 10px; }\n" +
            "        h1 { margin: 0; font-size: 24px; }\n" +
            "        h2 { color: #667eea; border-bottom: 2px solid #667eea; padding-bottom: 10px; }\n" +
            "        .unsubscribe { font-size: 12px; color: #666; margin-top: 20px; }\n" +
            "        .unsubscribe a { color: #667eea; text-decoration: none; }\n" +
            "    </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <div class=\"header\">\n" +
            "        <h1>🌟 Your Bi-weekly Mental Health Insights</h1>\n" +
            "        <p>Hi %s! Here's your personalized wellness report.</p>\n" +
            "    </div>\n" +
            "    <div class=\"content\">\n" +
            "        %s\n" +
            "    </div>\n" +
            "    <div class=\"footer\">\n" +
            "        <p>Keep up the great work with your journaling journey! 💪</p>\n" +
            "        <p><strong>Your Journal App Team</strong></p>\n" +
            "        <div class=\"unsubscribe\">\n" +
            "            <p>Don't want to receive these reports? <a href=\"#\">Unsubscribe here</a></p>\n" +
            "        </div>\n" +
            "    </div>\n" +
            "</body>\n" +
            "</html>",
            userName, reportContent);
    }

    private String createFallbackEmailTemplate(String userName) {
        return String.format(
            "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "    <title>Your Bi-weekly Mental Health Report</title>\n" +
            "    <style>\n" +
            "        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }\n" +
            "        .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }\n" +
            "        .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }\n" +
            "        .footer { text-align: center; margin-top: 30px; padding: 20px; background: #e9ecef; border-radius: 10px; }\n" +
            "    </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <div class=\"header\">\n" +
            "        <h1>🌟 Your Bi-weekly Mental Health Report</h1>\n" +
            "    </div>\n" +
            "    <div class=\"content\">\n" +
            "        <h2>Hi %s!</h2>\n" +
            "        <p>We encountered a temporary issue generating your personalized report, but we wanted to reach out anyway!</p>\n" +
            "        <p>Thank you for continuing to journal and prioritize your mental health. Regular journaling is a powerful tool for self-reflection and emotional well-being.</p>\n" +
            "        <p>We'll have your next detailed report ready in two weeks. Keep up the great work!</p>\n" +
            "    </div>\n" +
            "    <div class=\"footer\">\n" +
            "        <p>Take care,<br><strong>Your Journal App Team</strong></p>\n" +
            "    </div>\n" +
            "</body>\n" +
            "</html>",
            userName);
    }

    private String buildReportCacheKey(String userId) {
        LocalDateTime now = LocalDateTime.now();
        String period = now.getYear() + "-" + now.getDayOfYear() / 14; // Bi-weekly period identifier
        return "report:" + userId + ":" + period;
    }

    private boolean isReportRecentlyGenerated(String cacheKey) {
        try {
            String cachedReport = redisService.get(cacheKey, String.class);
            return cachedReport != null;
        } catch (Exception e) {
            return false;
        }
    }

    private void cacheReport(String cacheKey, String reportContent) {
        try {
            // Cache for 90 days (90 * 24 * 60 * 60 = 7776000 seconds)
            redisService.set(cacheKey, reportContent, 7776000L);
            log.debug("📋 Cached report content for key: {}", cacheKey);
        } catch (Exception e) {
            log.warn("⚠️ Failed to cache report: {}", e.getMessage());
        }
    }

    /**
     * Cache the timestamp when a report was generated
     */
    private void cacheReportTimestamp(String reportCacheKey) {
        try {
            String timestampKey = reportCacheKey + ":timestamp";
            String currentTimestamp = LocalDateTime.now().toString();
            // Cache timestamp for same duration as report (90 days)
            redisService.set(timestampKey, currentTimestamp, 7776000L);
            log.debug("📅 Cached report timestamp for key: {}", timestampKey);
        } catch (Exception e) {
            log.warn("⚠️ Failed to cache report timestamp: {}", e.getMessage());
        }
    }

    private <T> List<List<T>> createBatches(List<T> list, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            batches.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return batches;
    }
}
