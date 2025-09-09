package net.engineeringdigest.journalApp.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JournalEntryResponseDTO {

    private String id;
    private String title;
    private String content;
    private LocalDateTime date;
    private String collectionId;
    private String collectionName; // Include collection name for convenience

    // Sentiment Analysis Fields (optional - only included if user has sentiment analysis enabled)
    private Double sentimentScore; // Range: -1.0 (very negative) to 1.0 (very positive)
    private String sentimentLabel; // "positive", "negative", "neutral"
    private List<String> emotions; // ["joy", "sadness", "anxiety", etc.]
    private List<String> keywords; // Key themes extracted from the entry
    private LocalDateTime sentimentAnalyzedAt;
}