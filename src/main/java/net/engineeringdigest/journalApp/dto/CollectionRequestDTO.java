package net.engineeringdigest.journalApp.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@NoArgsConstructor
public class CollectionRequestDTO {

    @NotBlank(message = "Collection name is required")
    @Size(min = 1, max = 100, message = "Collection name must be between 1 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @Size(max = 7, message = "Color should be a valid hex color code")
    private String color;
}