package net.engineeringdigest.journalApp.services;

import lombok.extern.slf4j.Slf4j;
import net.engineeringdigest.journalApp.entity.JournalEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class SentimentAnalysisService {

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private RedisService redisService;

    /**
     * Analyze sentiment for a journal entry and update the entry with results
     */
    public void analyzeSentiment(JournalEntry entry) {
        try {
            // Check if already analyzed recently (avoid re-analysis)
            if (entry.getSentimentAnalyzedAt() != null &&
                entry.getSentimentAnalyzedAt().isAfter(LocalDateTime.now().minusHours(24))) {
                log.debug("Entry {} already analyzed recently, skipping", entry.getId());
                return;
            }

            // Check cache first
            String cacheKey = buildSentimentCacheKey(entry.getId().toHexString());
            GeminiService.SentimentAnalysisResult cachedResult = getCachedSentiment(cacheKey);

            if (cachedResult != null) {
                log.debug("Using cached sentiment for entry: {}", entry.getId());
                updateEntryWithSentiment(entry, cachedResult);
                return;
            }

            // Perform sentiment analysis
            log.debug("Analyzing sentiment for entry: {}", entry.getId());
            GeminiService.SentimentAnalysisResult result = geminiService.analyzeSentiment(
                entry.getTitle(), entry.getContent());

            // Update entry with results
            updateEntryWithSentiment(entry, result);

            // Cache the result
            cacheSentimentResult(cacheKey, result);

            log.debug("Sentiment analysis completed for entry: {}", entry.getId());
        } catch (Exception e) {
            log.error("Error analyzing sentiment for entry {}: {}", entry.getId(), e.getMessage());
            // Set neutral sentiment as fallback
            setFallbackSentiment(entry);
        }
    }

    /**
     * Legacy method for backward compatibility
     */
    @Deprecated
    public String getSentiment(String text) {
        try {
            GeminiService.SentimentAnalysisResult result = geminiService.analyzeSentiment("", text);
            return result.getSentimentLabel();
        } catch (Exception e) {
            log.error("Error in legacy getSentiment method: {}", e.getMessage());
            return "neutral";
        }
    }

    private void updateEntryWithSentiment(JournalEntry entry, GeminiService.SentimentAnalysisResult result) {
        entry.setSentimentScore(result.getSentimentScore());
        entry.setSentimentLabel(result.getSentimentLabel());
        entry.setEmotions(result.getEmotions());
        entry.setKeywords(result.getKeywords());
        entry.setSentimentAnalyzedAt(LocalDateTime.now());
    }

    private void setFallbackSentiment(JournalEntry entry) {
        entry.setSentimentScore(0.0);
        entry.setSentimentLabel("neutral");
        entry.setEmotions(Arrays.asList("unknown"));
        entry.setKeywords(Arrays.asList("journal", "entry"));
        entry.setSentimentAnalyzedAt(LocalDateTime.now());
    }

    private String buildSentimentCacheKey(String entryId) {
        return "sentiment:" + entryId;
    }

    private void cacheSentimentResult(String cacheKey, GeminiService.SentimentAnalysisResult result) {
        try {
            // Cache for 30 days (30 * 24 * 60 * 60 = 2592000 seconds)
            redisService.set(cacheKey, result, 2592000L);
        } catch (Exception e) {
            log.warn("Failed to cache sentiment result: {}", e.getMessage());
        }
    }

    private GeminiService.SentimentAnalysisResult getCachedSentiment(String cacheKey) {
        try {
            return redisService.get(cacheKey, GeminiService.SentimentAnalysisResult.class);
        } catch (Exception e) {
            log.debug("No cached sentiment found or error retrieving: {}", e.getMessage());
            return null;
        }
    }
}
