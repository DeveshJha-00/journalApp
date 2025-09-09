package net.engineeringdigest.journalApp.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@NoArgsConstructor
public class JournalEntryRequestDTO {

    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
    private String title;

    @Size(max = 10000, message = "Content cannot exceed 10000 characters")
    private String content;

    private String collectionId; // Optional - can be null
}