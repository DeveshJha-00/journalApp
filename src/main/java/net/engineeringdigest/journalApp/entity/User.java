package net.engineeringdigest.journalApp.entity;

import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.*;


@Document(collection = "users")
@CompoundIndexes({
        @CompoundIndex(name = "sentiment_email_idx", def = "{'sentimentAnalysis': 1, 'email': 1}")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    private ObjectId id;

    @Indexed(unique = true)
    @NonNull
    private String userName;

    @Indexed
    private String email;
    @Indexed
    private boolean sentimentAnalysis;

    private String password; // Nullable for OAuth users

    private String authProvider; // "LOCAL" or "GOOGLE", defaults to "LOCAL"

    @DBRef(lazy = true)
    private List<JournalEntry> journalEntryList = new ArrayList<>();

    private List<String> roles;

}
