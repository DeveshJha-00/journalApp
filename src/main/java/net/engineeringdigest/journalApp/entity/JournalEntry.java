package net.engineeringdigest.journalApp.entity;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;


@Document(collection = "journal_entries")
@Data
@NoArgsConstructor
public class JournalEntry {

    @Id
    private ObjectId id;
    @NonNull
    private String title;
    private String content;
    private LocalDateTime date;
    private ObjectId collectionId;

    // Sentiment Analysis Fields
    private Double sentimentScore; // Range: -1.0 (very negative) to 1.0 (very positive)
    private String sentimentLabel; // "positive", "negative", "neutral"
    private List<String> emotions; // ["joy", "sadness", "anxiety", etc.]
    private List<String> keywords; // Key themes extracted from the entry
    private LocalDateTime sentimentAnalyzedAt;

}
