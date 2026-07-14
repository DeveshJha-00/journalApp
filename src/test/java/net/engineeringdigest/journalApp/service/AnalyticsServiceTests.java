package net.engineeringdigest.journalApp.service;

import net.engineeringdigest.journalApp.dto.AnalyticsResponseDTO;
import net.engineeringdigest.journalApp.entity.JournalEntry;
import net.engineeringdigest.journalApp.entity.User;
import net.engineeringdigest.journalApp.repository.JournalEntryRepository;
import net.engineeringdigest.journalApp.services.AnalyticsService;
import net.engineeringdigest.journalApp.services.RedisService;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTests {

    @Mock
    private RedisService redisService;

    @Mock
    private JournalEntryRepository journalEntryRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Test
    void getAnalytics_allTimeIncludesOldAnalyzedEntries() {
        ObjectId userId = new ObjectId();
        when(redisService.get(eq("analytics:" + userId.toHexString() + ":30"), eq(AnalyticsResponseDTO.class)))
                .thenReturn(null);
        when(redisService.get(eq("analytics:" + userId.toHexString() + ":all"), eq(AnalyticsResponseDTO.class)))
                .thenReturn(null);

        JournalEntry oldEntry = entry(userId, LocalDateTime.now().minusDays(120), 0.2);
        JournalEntry recentEntry = entry(userId, LocalDateTime.now().minusDays(10), -0.2);

        when(journalEntryRepository.findByUserIdAndDateAfterOrderByDateAsc(eq(userId), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(recentEntry));
        when(journalEntryRepository.findByUserIdOrderByDateAsc(userId))
                .thenReturn(Arrays.asList(oldEntry, recentEntry));

        User user = User.builder()
                .id(userId)
                .userName("Ashu")
                .journalEntryList(Arrays.asList(oldEntry, recentEntry))
                .roles(Collections.singletonList("USER"))
                .build();

        AnalyticsResponseDTO thirtyDays = analyticsService.getAnalytics(user, "30d");
        AnalyticsResponseDTO allTime = analyticsService.getAnalytics(user, "all");

        assertEquals(1, thirtyDays.getTotalEntries());
        assertEquals(2, allTime.getTotalEntries());
        assertEquals(2, allTime.getMoodTimeline().size());
    }

    private JournalEntry entry(ObjectId userId, LocalDateTime date, double sentimentScore) {
        JournalEntry entry = new JournalEntry();
        entry.setId(new ObjectId());
        entry.setUserId(userId);
        entry.setTitle("Entry");
        entry.setContent("Content");
        entry.setDate(date);
        entry.setSentimentScore(sentimentScore);
        entry.setSentimentLabel(sentimentScore >= 0 ? "positive" : "negative");
        entry.setEmotions(Collections.singletonList("focused"));
        entry.setKeywords(Collections.singletonList("journal"));
        entry.setSentimentAnalyzedAt(date.plusMinutes(1));
        return entry;
    }
}
