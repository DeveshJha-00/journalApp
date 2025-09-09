package net.engineeringdigest.journalApp.dto;

import net.engineeringdigest.journalApp.entity.Collection;
import net.engineeringdigest.journalApp.entity.JournalEntry;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

@Component
public class DTOMapper {

    // Collection mappings
    public Collection toEntity(CollectionRequestDTO dto) {
        Collection collection = new Collection();
        collection.setName(dto.getName());
        collection.setDescription(dto.getDescription());
        collection.setColor(dto.getColor());
        return collection;
    }

    public CollectionResponseDTO toResponseDTO(Collection collection) {
        return new CollectionResponseDTO(
            collection.getId() != null ? collection.getId().toHexString() : null,
            collection.getName(),
            collection.getDescription(),
            collection.getColor(),
            collection.getCreatedDate()
        );
    }

    public void updateEntityFromDTO(Collection collection, CollectionRequestDTO dto) {
        if (dto.getName() != null && !dto.getName().trim().isEmpty()) {
            collection.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            collection.setDescription(dto.getDescription());
        }
        if (dto.getColor() != null) {
            collection.setColor(dto.getColor());
        }
    }

    // JournalEntry mappings
    public JournalEntry toEntity(JournalEntryRequestDTO dto) {
        JournalEntry entry = new JournalEntry();
        entry.setTitle(dto.getTitle());
        entry.setContent(dto.getContent());
        if (dto.getCollectionId() != null && !dto.getCollectionId().trim().isEmpty()) {
            entry.setCollectionId(new ObjectId(dto.getCollectionId()));
        }
        return entry;
    }

    public JournalEntryResponseDTO toResponseDTO(JournalEntry entry) {
        return toResponseDTO(entry, null);
    }

    public JournalEntryResponseDTO toResponseDTO(JournalEntry entry, String collectionName) {
        return new JournalEntryResponseDTO(
            entry.getId() != null ? entry.getId().toHexString() : null,
            entry.getTitle(),
            entry.getContent(),
            entry.getDate(),
            entry.getCollectionId() != null ? entry.getCollectionId().toHexString() : null,
            collectionName
        );
    }

    public void updateEntityFromDTO(JournalEntry entry, JournalEntryRequestDTO dto) {
        if (dto.getTitle() != null && !dto.getTitle().trim().isEmpty()) {
            entry.setTitle(dto.getTitle());
        }
        if (dto.getContent() != null) {
            entry.setContent(dto.getContent());
        }
        if (dto.getCollectionId() != null && !dto.getCollectionId().trim().isEmpty()) {
            entry.setCollectionId(new ObjectId(dto.getCollectionId()));
        } else if (dto.getCollectionId() != null && dto.getCollectionId().trim().isEmpty()) {
            entry.setCollectionId(null);
        }
    }
}