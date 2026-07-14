package net.engineeringdigest.journalApp.entity;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;


@Document(collection = "journal_entries")
@CompoundIndexes({
        @CompoundIndex(name = "user_date_idx", def = "{'userId': 1, 'date': -1}"),
        @CompoundIndex(name = "user_collection_idx", def = "{'userId': 1, 'collectionId': 1}")
})
@Data
@NoArgsConstructor
public class JournalEntry {

    @Id
    private ObjectId id;
    @NonNull
    private String title;
    private String content;
    private LocalDateTime date;
    @Indexed
    private ObjectId userId;
    private ObjectId collectionId;

    // Sentiment Analysis Fields
    private Double sentimentScore; // Range: -1.0 (very negative) to 1.0 (very positive)
    private String sentimentLabel; // "positive", "negative", "neutral"
    private List<String> emotions; // ["joy", "sadness", "anxiety", etc.]
    private List<String> keywords; // Key themes extracted from the entry
    private LocalDateTime sentimentAnalyzedAt;

}
