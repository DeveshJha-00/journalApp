package net.engineeringdigest.journalApp.services;

import lombok.extern.slf4j.Slf4j;
import net.engineeringdigest.journalApp.dto.AnalyticsResponseDTO;
import net.engineeringdigest.journalApp.entity.JournalEntry;
import net.engineeringdigest.journalApp.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AnalyticsService {

    @Autowired
    private RedisService redisService;

    private static final long ANALYTICS_CACHE_TTL = 600; // 10 minutes
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Maps sentiment score (-1.0 to 1.0) to mood scale (1 to 10).
     * Formula: (score + 1) * 4.5 + 1
     * -1.0 → 1.0, 0.0 → 5.5, 1.0 → 10.0
     */
    public static double sentimentToMood(double sentimentScore) {
        double mood = (sentimentScore + 1.0) * 4.5 + 1.0;
        return Math.round(mood * 10.0) / 10.0; // Round to 1 decimal
    }

    /**
     * Parse a range string like "15d", "30d", "7d" into number of days.
     * Defaults to 15 if unparseable.
     */
    private int parseRangeDays(String range) {
        if (range == null || range.isBlank()) return 15;
        try {
            String cleaned = range.trim().toLowerCase().replace("d", "");
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            log.warn("Invalid range format '{}', defaulting to 15 days", range);
            return 15;
        }
    }

    private boolean isAllTimeRange(String range) {
        return range != null && "all".equalsIgnoreCase(range.trim());
    }

    /**
     * Compute analytics for a user over the given range.
     */
    public AnalyticsResponseDTO getAnalytics(User user, String range) {
        boolean allTime = isAllTimeRange(range);
        int days = allTime ? 0 : parseRangeDays(range);
        String userId = user.getId().toHexString();

        // Try cache first
        String cacheKey = "analytics:" + userId + ":" + (allTime ? "all" : days);
        AnalyticsResponseDTO cached = redisService.get(cacheKey, AnalyticsResponseDTO.class);
        if (cached != null) {
            log.debug("Analytics cache hit for user {} range {}", userId, allTime ? "all" : days + "d");
            return cached;
        }

        // Get all entries within range, or every dated entry for all-time analytics.
        LocalDateTime cutoff = allTime ? null : LocalDateTime.now().minusDays(days);
        List<JournalEntry> allEntriesInRange = user.getJournalEntryList() == null
                ? Collections.emptyList()
                : user.getJournalEntryList().stream()
                .filter(e -> e.getDate() != null)
                .filter(e -> allTime || e.getDate().isAfter(cutoff))
                .sorted(Comparator.comparing(JournalEntry::getDate))
                .collect(Collectors.toList());

        int totalEntries = allEntriesInRange.size();

        // Entries per day (based on ALL entries, not just sentiment-analyzed)
        double entriesPerDay = 0.0;
        if (totalEntries > 0) {
            long denominatorDays;
            if (allTime) {
                LocalDate firstEntryDate = allEntriesInRange.get(0).getDate().toLocalDate();
                denominatorDays = ChronoUnit.DAYS.between(firstEntryDate, LocalDate.now()) + 1;
            } else {
                denominatorDays = days;
            }
            entriesPerDay = denominatorDays > 0
                    ? Math.round((double) totalEntries / denominatorDays * 10.0) / 10.0
                    : 0.0;
        }

        // Filter to only entries with sentiment data for mood metrics
        List<JournalEntry> sentimentEntries = allEntriesInRange.stream()
                .filter(e -> e.getSentimentScore() != null && e.getSentimentAnalyzedAt() != null)
                .collect(Collectors.toList());

        // Average mood (from sentiment-analyzed entries only)
        double avgMood = 0.0;
        if (!sentimentEntries.isEmpty()) {
            double avgSentiment = sentimentEntries.stream()
                    .mapToDouble(JournalEntry::getSentimentScore)
                    .average()
                    .orElse(0.0);
            avgMood = sentimentToMood(avgSentiment);
        }

        // Mood timeline — group sentiment entries by date
        Map<LocalDate, List<JournalEntry>> byDate = sentimentEntries.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getDate().toLocalDate(),
                        TreeMap::new,
                        Collectors.toList()
                ));

        List<AnalyticsResponseDTO.DailyMood> moodTimeline = byDate.entrySet().stream()
                .map(entry -> {
                    double dayAvgSentiment = entry.getValue().stream()
                            .mapToDouble(JournalEntry::getSentimentScore)
                            .average()
                            .orElse(0.0);
                    return AnalyticsResponseDTO.DailyMood.builder()
                            .date(entry.getKey().format(DATE_FMT))
                            .avgMood(sentimentToMood(dayAvgSentiment))
                            .entries(entry.getValue().size())
                            .build();
                })
                .collect(Collectors.toList());

        // Top emotions — frequency sorted, top 5
        List<String> topEmotions = sentimentEntries.stream()
                .filter(e -> e.getEmotions() != null)
                .flatMap(e -> e.getEmotions().stream())
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // Top keywords — frequency sorted, top 5
        List<String> topKeywords = sentimentEntries.stream()
                .filter(e -> e.getKeywords() != null)
                .flatMap(e -> e.getKeywords().stream())
                .collect(Collectors.groupingBy(s -> s.toLowerCase(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        AnalyticsResponseDTO result = AnalyticsResponseDTO.builder()
                .totalEntries(totalEntries)
                .avgMood(avgMood)
                .entriesPerDay(entriesPerDay)
                .moodTimeline(moodTimeline)
                .topEmotions(topEmotions)
                .topKeywords(topKeywords)
                .build();

        // Cache result
        try {
            redisService.set(cacheKey, result, ANALYTICS_CACHE_TTL);
        } catch (Exception e) {
            log.warn("Failed to cache analytics for user {}: {}", userId, e.getMessage());
        }

        return result;
    }
}
