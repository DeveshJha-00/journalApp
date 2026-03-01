package net.engineeringdigest.journalApp.controller;
import lombok.extern.slf4j.Slf4j;
import net.engineeringdigest.journalApp.dto.JournalEntryRequestDTO;
import net.engineeringdigest.journalApp.dto.JournalEntryResponseDTO;
import net.engineeringdigest.journalApp.dto.DTOMapper;
import net.engineeringdigest.journalApp.entity.Collection;
import net.engineeringdigest.journalApp.entity.JournalEntry;
import net.engineeringdigest.journalApp.entity.User;
import net.engineeringdigest.journalApp.services.CollectionService;
import net.engineeringdigest.journalApp.services.JournalEntryService;
import net.engineeringdigest.journalApp.services.UserService;
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
@RequestMapping("/journals")
@Slf4j
public class JournalEntryController {

    @Autowired
    private JournalEntryService journalEntryService;

    @Autowired
    private UserService userService;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private DTOMapper dtoMapper;


    @GetMapping()
    public ResponseEntity<List<JournalEntryResponseDTO>> getAllEntriesOfUser(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();
        User user = userService.findByUsername(userName);
        List<JournalEntry> journalEntriesofUser = user.getJournalEntryList();
        if (journalEntriesofUser!=null && !journalEntriesofUser.isEmpty()){
            List<JournalEntryResponseDTO> responseDTOs = journalEntriesofUser.stream()
                    .map(entry -> {
                        String collectionName = null;
                        if (entry.getCollectionId() != null) {
                            Optional<Collection> collection = collectionService.getCollectionById(entry.getCollectionId());
                            collectionName = collection.map(Collection::getName).orElse(null);
                        }
                        return dtoMapper.toResponseDTO(entry, collectionName);
                    })
                    .collect(Collectors.toList());
            return new ResponseEntity<>(responseDTOs, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping("/id/{targetId}")
    public ResponseEntity<JournalEntryResponseDTO> getEntryById(@PathVariable ObjectId targetId){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();
        User user = userService.findByUsername(userName);
        List<JournalEntry> collect = user.getJournalEntryList().stream().filter(x -> x.getId().equals(targetId)).collect(Collectors.toList());
        if (!collect.isEmpty()){
            Optional<JournalEntry> journalEntry =  journalEntryService.getEntryById(targetId);
            if (journalEntry.isPresent()){
                JournalEntry entry = journalEntry.get();
                String collectionName = null;
                if (entry.getCollectionId() != null) {
                    Optional<Collection> collection = collectionService.getCollectionById(entry.getCollectionId());
                    collectionName = collection.map(Collection::getName).orElse(null);
                }
                JournalEntryResponseDTO responseDTO = dtoMapper.toResponseDTO(entry, collectionName);
                return new ResponseEntity<>(responseDTO, HttpStatus.OK);
            }
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PostMapping() // (C)reate
    public  ResponseEntity<JournalEntryResponseDTO>  postEntries(@Valid @RequestBody JournalEntryRequestDTO entryRequestDTO){
        try {
            log.info("📝 Creating journal entry for title: '{}'", entryRequestDTO.getTitle());

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userName = authentication.getName();
            log.info("👤 Authenticated user: {}", userName);

            JournalEntry entry = dtoMapper.toEntity(entryRequestDTO);
            log.info("🔄 Mapped DTO to entity, calling saveEntry service");

            // journalEntryService.saveEntry(entry, userName);
            journalEntryService.createEntry(entry, userName);
            log.info("✅ Journal entry saved successfully with ID: {}", entry.getId());

            String collectionName = null;
            if (entry.getCollectionId() != null) {
                Optional<Collection> collection = collectionService.getCollectionById(entry.getCollectionId());
                collectionName = collection.map(Collection::getName).orElse(null);
                log.info("📁 Collection resolved: {}", collectionName);
            }

            JournalEntryResponseDTO responseDTO = dtoMapper.toResponseDTO(entry, collectionName);
            log.info("✅ Journal entry created successfully for user: {}", userName);
            return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
        } catch (Exception e){
            log.error("❌ Error creating journal entry for user: {}",
                     SecurityContextHolder.getContext().getAuthentication().getName(), e);
            log.error("📋 Request details - Title: '{}', Content length: {}, CollectionId: '{}'",
                     entryRequestDTO.getTitle(),
                     entryRequestDTO.getContent() != null ? entryRequestDTO.getContent().length() : 0,
                     entryRequestDTO.getCollectionId());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }


    @DeleteMapping("/id/{targetId}") // (D)elete
    public ResponseEntity<?> deleteEntryById(@PathVariable ObjectId targetId){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();
        boolean removed = journalEntryService.deleteById(targetId, userName);
        if (removed)
            return  new ResponseEntity<>(HttpStatus.NO_CONTENT);
        else
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }


    @PutMapping("/id/{targetId}") // (U)pdate
    public ResponseEntity<JournalEntryResponseDTO> updateEntry(@PathVariable ObjectId targetId, @Valid @RequestBody JournalEntryRequestDTO entryRequestDTO){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();
        User user = userService.findByUsername(userName);
        List<JournalEntry> collect = user.getJournalEntryList().stream().filter(x -> x.getId().equals(targetId)).collect(Collectors.toList());
        if (!collect.isEmpty()){
            Optional<JournalEntry> journalEntry =  journalEntryService.getEntryById(targetId);
            if (journalEntry.isPresent()){
                JournalEntry entry = journalEntry.get();
                dtoMapper.updateEntityFromDTO(entry, entryRequestDTO);
                // journalEntryService.saveEntry(entry, userName);
                journalEntryService.updateEntry(entry, userName);

                String collectionName = null;
                if (entry.getCollectionId() != null) {
                    Optional<Collection> collection = collectionService.getCollectionById(entry.getCollectionId());
                    collectionName = collection.map(Collection::getName).orElse(null);
                }
                JournalEntryResponseDTO responseDTO = dtoMapper.toResponseDTO(entry, collectionName);
                return new ResponseEntity<>(responseDTO, HttpStatus.OK);
            }
        }

        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PutMapping("/id/{targetId}/collection/{collectionId}")
    public ResponseEntity<?> assignToCollection(@PathVariable ObjectId targetId, @PathVariable ObjectId collectionId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();
        boolean assigned = journalEntryService.assignToCollection(targetId, collectionId, userName);
        if (assigned) {
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/id/{targetId}/collection")
    public ResponseEntity<?> removeFromCollection(@PathVariable ObjectId targetId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();
        boolean assigned = journalEntryService.assignToCollection(targetId, null, userName);
        if (assigned) {
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }


}

