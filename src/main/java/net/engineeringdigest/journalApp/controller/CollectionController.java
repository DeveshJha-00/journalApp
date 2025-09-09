package net.engineeringdigest.journalApp.controller;

import net.engineeringdigest.journalApp.dto.CollectionRequestDTO;
import net.engineeringdigest.journalApp.dto.CollectionResponseDTO;
import net.engineeringdigest.journalApp.dto.JournalEntryResponseDTO;
import net.engineeringdigest.journalApp.dto.DTOMapper;
import net.engineeringdigest.journalApp.entity.Collection;
import net.engineeringdigest.journalApp.entity.JournalEntry;
import net.engineeringdigest.journalApp.services.CollectionService;
import net.engineeringdigest.journalApp.services.JournalEntryService;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/collections")
public class CollectionController {

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private JournalEntryService journalEntryService;

    @Autowired
    private DTOMapper dtoMapper;

    @GetMapping()
    public ResponseEntity<List<CollectionResponseDTO>> getAllCollectionsOfUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();
        List<Collection> collections = collectionService.getCollectionsByUsername(userName);
        if (collections != null && !collections.isEmpty()) {
            List<CollectionResponseDTO> responseDTOs = collections.stream()
                    .map(dtoMapper::toResponseDTO)
                    .collect(Collectors.toList());
            return new ResponseEntity<>(responseDTOs, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping("/id/{targetId}")
    public ResponseEntity<CollectionResponseDTO> getCollectionById(@PathVariable ObjectId targetId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();

        if (!collectionService.isCollectionOwnedByUser(targetId, userName)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        Optional<Collection> collection = collectionService.getCollectionById(targetId);
        if (collection.isPresent()) {
            CollectionResponseDTO responseDTO = dtoMapper.toResponseDTO(collection.get());
            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PostMapping() // (C)reate
    public ResponseEntity<CollectionResponseDTO> createCollection(@Valid @RequestBody CollectionRequestDTO collectionRequestDTO) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userName = authentication.getName();
            Collection collection = dtoMapper.toEntity(collectionRequestDTO);
            Collection savedCollection = collectionService.saveCollection(collection, userName);
            CollectionResponseDTO responseDTO = dtoMapper.toResponseDTO(savedCollection);
            return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/id/{targetId}") // (U)pdate
    public ResponseEntity<CollectionResponseDTO> updateCollection(@PathVariable ObjectId targetId, @Valid @RequestBody CollectionRequestDTO collectionRequestDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();

        if (!collectionService.isCollectionOwnedByUser(targetId, userName)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        Optional<Collection> collectionOpt = collectionService.getCollectionById(targetId);
        if (collectionOpt.isPresent()) {
            Collection collection = collectionOpt.get();
            dtoMapper.updateEntityFromDTO(collection, collectionRequestDTO);

            Collection updatedCollection = collectionService.saveCollection(collection, userName);
            CollectionResponseDTO responseDTO = dtoMapper.toResponseDTO(updatedCollection);
            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/id/{targetId}") // (D)elete
    public ResponseEntity<?> deleteCollectionById(@PathVariable ObjectId targetId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();
        boolean removed = collectionService.deleteById(targetId, userName);
        if (removed) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/id/{targetId}/entries")
    public ResponseEntity<List<JournalEntryResponseDTO>> getEntriesInCollection(@PathVariable ObjectId targetId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();

        if (!collectionService.isCollectionOwnedByUser(targetId, userName)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        // Get collection name for the response
        Optional<Collection> collection = collectionService.getCollectionById(targetId);
        String collectionName = collection.map(Collection::getName).orElse(null);

        List<JournalEntry> entries = journalEntryService.getEntriesByCollectionId(targetId, userName);
        if (entries != null && !entries.isEmpty()) {
            List<JournalEntryResponseDTO> responseDTOs = entries.stream()
                    .map(entry -> dtoMapper.toResponseDTO(entry, collectionName))
                    .collect(Collectors.toList());
            return new ResponseEntity<>(responseDTOs, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
}