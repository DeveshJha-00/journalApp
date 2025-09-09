package net.engineeringdigest.journalApp.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

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
}