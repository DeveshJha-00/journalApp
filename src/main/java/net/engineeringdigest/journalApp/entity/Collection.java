
package net.engineeringdigest.journalApp.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "collections")
@Data
@NoArgsConstructor
public class Collection {

    @Id
    private ObjectId id;

    @NonNull
    private String name;

    private String description;

    @NonNull
    private ObjectId userId;

    private LocalDateTime createdDate;
}