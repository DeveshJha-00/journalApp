package net.engineeringdigest.journalApp.services;

import lombok.extern.slf4j.Slf4j;
import net.engineeringdigest.journalApp.entity.BiweeklyReport;
import net.engineeringdigest.journalApp.entity.JournalEntry;
import net.engineeringdigest.journalApp.entity.User;
import net.engineeringdigest.journalApp.repository.BiweeklyReportRepository;
import net.engineeringdigest.journalApp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import net.engineeringdigest.journalApp.repository.JournalEntryRepository;

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

    @Autowired
    private BiweeklyReportRepository biweeklyReportRepository;

    @Autowired
    private SentimentAnalysisService sentimentAnalysisService;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @Value("${report.frequency.days:14}")
    private int reportFrequencyDays;

    @Value("${report.min.entries:2}")
    private int minEntriesForReport;

    @Value("${report.max.entries:50}")
    private int maxEntriesPerReport;

    @Value("${report.email.subject:Your Bi-weekly Mental Health Insights}")
    private String reportEmailSubject;


    @Scheduled(cron = "0 0 9 */14 * ?")
    @SchedulerLock(name = "biweeklyReport", lockAtLeastFor = "PT1H")
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
                    reportContent = generateFreshReport(recentEntries, user, reportCacheKey);
                }
            } else {
                // New entries exist or no cached report - generate fresh report
                log.info("🆕 Generating fresh report for user: {} ({} new entries)", user.getUserName(), newEntries.size());
                reportContent = generateFreshReport(recentEntries, user, reportCacheKey);
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
                return normalizeReportContent(cachedContent);
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
        List<JournalEntry> entries = timestamp == null
                ? journalEntryRepository.findByUserIdOrderByDateDesc(user.getId())
                : journalEntryRepository.findByUserIdAndDateAfterOrderByDateDesc(user.getId(), timestamp);

        List<JournalEntry> legacyEntries = getLegacyEntriesForUser(user);
        if (!legacyEntries.isEmpty() && legacyEntries.size() > entries.size()) {
            entries = timestamp == null
                    ? journalEntryRepository.findByUserIdOrderByDateDesc(user.getId())
                    : journalEntryRepository.findByUserIdAndDateAfterOrderByDateDesc(user.getId(), timestamp);
        }
        if (entries.isEmpty()) {
            entries = getLegacyEntriesForUser(user);
        }

        return entries.stream()
                .filter(entry -> entry.getDate() != null)
                .filter(entry -> timestamp == null || entry.getDate().isAfter(timestamp))
                .filter(entry -> entry.getSentimentAnalyzedAt() != null) // Only entries with sentiment analysis
                .collect(Collectors.toList());
    }

    /**
     * Generate fresh report content using Gemini API, persist to MongoDB, and cache it
     */
    private String generateFreshReport(List<JournalEntry> recentEntries, User user, String reportCacheKey) {
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
            String reportContent = normalizeReportContent(
                    geminiService.generateBiweeklyReport(entriesData, user.getUserName()));
            log.info("✅ Fresh report generated (length: {} chars)", reportContent.length());

            // Cache the new report with timestamp
            cacheReport(reportCacheKey, reportContent);
            cacheReportTimestamp(reportCacheKey);

            // Persist report to MongoDB
            persistReport(user, entriesToAnalyze, reportContent);

            return reportContent;
        } catch (Exception e) {
            log.error("❌ Failed to generate fresh report: {}", e.getMessage());
            throw new RuntimeException("Failed to generate fresh report", e);
        }
    }

    /**
     * Persist a generated report to MongoDB for dashboard access
     */
    private void persistReport(User user, List<JournalEntry> entries, String reportContent) {
        try {
            // Compute summary metrics from entries
            double avgSentiment = entries.stream()
                    .filter(e -> e.getSentimentScore() != null)
                    .mapToDouble(JournalEntry::getSentimentScore)
                    .average()
                    .orElse(0.0);

            List<String> topEmotions = entries.stream()
                    .filter(e -> e.getEmotions() != null)
                    .flatMap(e -> e.getEmotions().stream())
                    .collect(Collectors.groupingBy(s -> s, Collectors.counting()))
                    .entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(5)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            List<String> topKeywords = entries.stream()
                    .filter(e -> e.getKeywords() != null)
                    .flatMap(e -> e.getKeywords().stream())
                    .collect(Collectors.groupingBy(s -> s.toLowerCase(), Collectors.counting()))
                    .entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(5)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            // Determine period boundaries from entries
            LocalDateTime periodStart = entries.stream()
                    .map(JournalEntry::getDate)
                    .filter(Objects::nonNull)
                    .min(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now().minusDays(reportFrequencyDays));

            LocalDateTime periodEnd = entries.stream()
                    .map(JournalEntry::getDate)
                    .filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now());

            BiweeklyReport report = BiweeklyReport.builder()
                    .userId(user.getId())
                    .reportContent(reportContent)
                    .periodStart(periodStart)
                    .periodEnd(periodEnd)
                    .avgSentimentScore(avgSentiment)
                    .totalEntries(entries.size())
                    .topEmotions(topEmotions)
                    .topKeywords(topKeywords)
                    .generatedAt(LocalDateTime.now())
                    .build();

            biweeklyReportRepository.save(report);
            log.info("💾 Persisted bi-weekly report to MongoDB for user: {} (entries: {}, avgSentiment: {})",
                     user.getUserName(), entries.size(), String.format("%.2f", avgSentiment));
        } catch (Exception e) {
            log.error("⚠️ Failed to persist report to MongoDB for user {}: {}", user.getUserName(), e.getMessage());
            // Don't fail the overall report generation if persistence fails
        }
    }

    private List<User> getEligibleUsers() {
        return userRepository.findEligibleForBiweeklyReports();
    }

    private List<JournalEntry> getRecentEntriesWithSentiment(User user, int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);

        List<JournalEntry> allEntries = journalEntryRepository.findByUserIdAndDateAfterOrderByDateDesc(user.getId(), cutoffDate);
        List<JournalEntry> legacyEntries = getLegacyEntriesForUser(user);
        if (!legacyEntries.isEmpty() && legacyEntries.size() > allEntries.size()) {
            allEntries = journalEntryRepository.findByUserIdAndDateAfterOrderByDateDesc(user.getId(), cutoffDate);
        }
        if (allEntries.isEmpty()) {
            allEntries = legacyEntries.stream()
                    .filter(entry -> entry.getDate() != null && entry.getDate().isAfter(cutoffDate))
                    .collect(Collectors.toList());
        }
        log.info("🔍 User {} has {} total entries in journalEntryList (cutoff: {})",
                 user.getUserName(), allEntries.size(), cutoffDate);

        List<JournalEntry> afterDateFilter = allEntries.stream()
                .filter(entry -> entry.getDate() != null && entry.getDate().isAfter(cutoffDate))
                .collect(Collectors.toList());
        log.info("🔍 After date filter (last {} days): {} entries", days, afterDateFilter.size());

        List<JournalEntry> afterSentimentFilter = afterDateFilter.stream()
                .filter(entry -> entry.getSentimentAnalyzedAt() != null)
                .collect(Collectors.toList());
        log.info("🔍 After sentimentAnalyzedAt filter: {} entries", afterSentimentFilter.size());

        if (afterDateFilter.size() > 0 && afterSentimentFilter.isEmpty()) {
            log.warn("⚠️ Entries exist but lack sentimentAnalyzedAt — falling back to entries with sentimentScore");
            afterSentimentFilter = afterDateFilter.stream()
                    .filter(entry -> entry.getSentimentScore() != null)
                    .collect(Collectors.toList());
            log.info("🔍 After sentimentScore fallback filter: {} entries", afterSentimentFilter.size());
        }

        // If entries still lack sentiment data, analyze them on the fly
        if (afterDateFilter.size() > 0 && afterSentimentFilter.isEmpty()) {
            log.info("🔄 Running on-the-fly sentiment analysis for {} entries", afterDateFilter.size());
            for (JournalEntry entry : afterDateFilter) {
                try {
                    sentimentAnalysisService.analyzeSentiment(entry);
                    journalEntryRepository.save(entry);
                    log.debug("✅ Analyzed sentiment for entry: {}", entry.getId());
                } catch (Exception e) {
                    log.warn("⚠️ Failed sentiment analysis for entry {}: {}", entry.getId(), e.getMessage());
                }
            }
            // Re-filter after analysis
            afterSentimentFilter = afterDateFilter.stream()
                    .filter(entry -> entry.getSentimentScore() != null || entry.getSentimentAnalyzedAt() != null)
                    .collect(Collectors.toList());
            log.info("🔍 After on-the-fly analysis: {} entries with sentiment data", afterSentimentFilter.size());
        }

        return afterSentimentFilter.stream()
                .sorted((e1, e2) -> e2.getDate().compareTo(e1.getDate()))
                .collect(Collectors.toList());
    }

    private List<JournalEntry> getLegacyEntriesForUser(User user) {
        if (user.getJournalEntryList() == null || user.getJournalEntryList().isEmpty()) {
            return Collections.emptyList();
        }

        List<JournalEntry> entriesToBackfill = user.getJournalEntryList().stream()
                .filter(entry -> entry.getUserId() == null)
                .peek(entry -> entry.setUserId(user.getId()))
                .collect(Collectors.toList());

        if (!entriesToBackfill.isEmpty()) {
            journalEntryRepository.saveAll(entriesToBackfill);
            log.info("Backfilled userId on {} legacy report entries for user: {}",
                    entriesToBackfill.size(), user.getUserName());
        }

        return user.getJournalEntryList();
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

    private String normalizeReportContent(String reportContent) {
        if (reportContent == null) {
            return null;
        }

        String cleaned = reportContent.trim()
                .replaceAll("(?is)^\\s*(?:```|'''|~~~)\\s*(?:html)?\\s*", "")
                .replaceAll("(?is)\\s*(?:```|'''|~~~)\\s*$", "")
                .replaceAll("(?is)<!doctype[^>]*>", "")
                .replaceAll("(?is)<head[\\s\\S]*?</head>", "")
                .replaceAll("(?is)<style[\\s\\S]*?</style>", "")
                .replaceAll("(?is)</?html[^>]*>", "");

        java.util.regex.Matcher bodyMatcher = java.util.regex.Pattern
                .compile("(?is)<body[^>]*>([\\s\\S]*?)</body>")
                .matcher(cleaned);
        if (bodyMatcher.find()) {
            cleaned = bodyMatcher.group(1);
        } else {
            cleaned = cleaned.replaceAll("(?is)</?body[^>]*>", "");
        }

        return cleaned.trim();
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
            "        .content { background: #ffffff; padding: 30px; border: 1px solid #e5e7eb; border-top: 0; border-radius: 0 0 10px 10px; color: #243447; }\n" +
            "        .content * { color: #243447 !important; }\n" +
            "        .content h1, .content h2, .content h3, .content h4 { color: #c2410c !important; line-height: 1.3; margin: 24px 0 12px; }\n" +
            "        .content h1:first-child, .content h2:first-child, .content h3:first-child { margin-top: 0; }\n" +
            "        .content p { margin: 0 0 16px; }\n" +
            "        .content ul, .content ol { margin: 0 0 16px 20px; padding-left: 18px; }\n" +
            "        .content li { margin-bottom: 8px; }\n" +
            "        .content strong, .content b { color: #111827 !important; }\n" +
            "        .content a { color: #ea580c !important; }\n" +
            "        .content blockquote { border-left: 4px solid #fb923c; margin: 16px 0; padding-left: 16px; color: #4b5563 !important; }\n" +
            "        .footer { text-align: center; margin-top: 30px; padding: 20px; background: #e9ecef; color: #374151; border-radius: 10px; }\n" +
            "        .footer * { color: #374151 !important; }\n" +
            "        h1 { margin: 0; font-size: 24px; }\n" +
            "        h2 { border-bottom: 2px solid #fed7aa; padding-bottom: 10px; }\n" +
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
