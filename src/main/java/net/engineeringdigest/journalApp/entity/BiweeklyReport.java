package net.engineeringdigest.journalApp.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "biweekly_reports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BiweeklyReport {

    @Id
    private ObjectId id;

    @Indexed
    private ObjectId userId;

    private String reportContent; // Full HTML content of the report

    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;

    private Double avgSentimentScore; // Average sentiment score for the period (-1.0 to 1.0)
    private int totalEntries;         // Number of entries analyzed

    private List<String> topEmotions; // Top emotions from the period
    private List<String> topKeywords; // Top keywords from the period

    private LocalDateTime generatedAt;
}
