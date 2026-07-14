package net.engineeringdigest.journalApp.service;

import net.engineeringdigest.journalApp.dto.AnalyticsResponseDTO;
import net.engineeringdigest.journalApp.entity.JournalEntry;
import net.engineeringdigest.journalApp.entity.User;
import net.engineeringdigest.journalApp.repository.JournalEntryRepository;
import net.engineeringdigest.journalApp.services.AnalyticsService;
import net.engineeringdigest.journalApp.services.RedisService;
import net.engineeringdigest.journalApp.services.UserService;
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

    @Mock
    private UserService userService;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Test
    void getAnalytics_allTimeIncludesOldAnalyzedEntries() {
        ObjectId userId = new ObjectId();
        when(redisService.get(eq("analytics:v2:" + userId.toHexString() + ":30"), eq(AnalyticsResponseDTO.class)))
                .thenReturn(null);
        when(redisService.get(eq("analytics:v2:" + userId.toHexString() + ":all"), eq(AnalyticsResponseDTO.class)))
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
        when(userService.findByUsernameFresh("Ashu")).thenReturn(user);

        AnalyticsResponseDTO thirtyDays = analyticsService.getAnalytics(user, "30d");
        AnalyticsResponseDTO allTime = analyticsService.getAnalytics(user, "all");

        assertEquals(1, thirtyDays.getTotalEntries());
        assertEquals(2, allTime.getTotalEntries());
        assertEquals(2, allTime.getMoodTimeline().size());
    }

    @Test
    void getAnalytics_backfillsLegacyEntriesFromFreshMongoUser() {
        ObjectId userId = new ObjectId();
        when(redisService.get(eq("analytics:v2:" + userId.toHexString() + ":all"), eq(AnalyticsResponseDTO.class)))
                .thenReturn(null);

        JournalEntry oldLegacyEntry = entry(null, LocalDateTime.now().minusDays(120), 0.4);

        User cachedUser = User.builder()
                .id(userId)
                .userName("Ashu")
                .roles(Collections.singletonList("USER"))
                .build();
        User freshUser = User.builder()
                .id(userId)
                .userName("Ashu")
                .journalEntryList(Collections.singletonList(oldLegacyEntry))
                .roles(Collections.singletonList("USER"))
                .build();

        when(journalEntryRepository.findByUserIdOrderByDateAsc(userId))
                .thenReturn(Collections.emptyList());
        when(userService.findByUsernameFresh("Ashu")).thenReturn(freshUser);

        AnalyticsResponseDTO allTime = analyticsService.getAnalytics(cachedUser, "all");

        assertEquals(1, allTime.getTotalEntries());
        assertEquals(userId, oldLegacyEntry.getUserId());
        assertEquals(1, allTime.getMoodTimeline().size());
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
