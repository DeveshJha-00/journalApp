package net.engineeringdigest.journalApp.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollectionResponseDTO {

    private String id;
    private String name;
    private String description;
    private String color;
    private LocalDateTime createdDate;
}