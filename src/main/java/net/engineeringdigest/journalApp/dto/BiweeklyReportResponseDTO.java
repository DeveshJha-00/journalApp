package net.engineeringdigest.journalApp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BiweeklyReportResponseDTO {

    private String id;
    private String reportContent;

    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;

    private Double avgSentimentScore;
    private Double avgMood; // Mapped to 1-10 scale for frontend
    private int totalEntries;

    private List<String> topEmotions;
    private List<String> topKeywords;

    private LocalDateTime generatedAt;
}
