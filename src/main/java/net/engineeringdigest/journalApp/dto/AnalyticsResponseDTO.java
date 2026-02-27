package net.engineeringdigest.journalApp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsResponseDTO {

    private int totalEntries;
    private double avgMood;       // 1-10 scale (mapped from sentimentScore)
    private double entriesPerDay;

    private List<DailyMood> moodTimeline;
    private List<String> topEmotions;
    private List<String> topKeywords;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyMood {
        private String date;    // "yyyy-MM-dd"
        private double avgMood; // 1-10 scale
        private int entries;
    }
}
