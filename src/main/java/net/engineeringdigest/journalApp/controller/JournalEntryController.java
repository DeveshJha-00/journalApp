package net.engineeringdigest.journalApp.controller;
import net.engineeringdigest.journalApp.entity.JournalEntry;
import net.engineeringdigest.journalApp.entity.User;
import net.engineeringdigest.journalApp.services.JournalEntryService;
import net.engineeringdigest.journalApp.services.UserService;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/journals")
public class JournalEntryController {

    @Autowired
    private JournalEntryService journalEntryService;

    @Autowired
    private UserService userService;


    @GetMapping()
    public ResponseEntity<List<JournalEntry>> getAllEntriesOfUser(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();
        User user = userService.findByUsername(userName);
        List<JournalEntry> journalEntriesofUser = user.getJournalEntryList();
        if (journalEntriesofUser!=null && !journalEntriesofUser.isEmpty()){
            return new ResponseEntity<>(journalEntriesofUser, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping("/id/{targetId}")
    public ResponseEntity<JournalEntry> getEntryById(@PathVariable ObjectId targetId){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();
        User user = userService.findByUsername(userName);
        List<JournalEntry> collect = user.getJournalEntryList().stream().filter(x -> x.getId().equals(targetId)).collect(Collectors.toList());
        if (!collect.isEmpty()){
            Optional<JournalEntry> journalEntry =  journalEntryService.getEntryById(targetId);
            if (journalEntry.isPresent()){
                return new ResponseEntity<>(journalEntry.get(), HttpStatus.OK);
            }
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PostMapping() // (C)reate
    public  ResponseEntity<JournalEntry>  postEntries(@RequestBody JournalEntry entry){
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userName = authentication.getName();
            journalEntryService.saveEntry(entry, userName);
            return new ResponseEntity<>(entry, HttpStatus.CREATED);
        } catch (Exception e){
            return new ResponseEntity<>(entry, HttpStatus.BAD_REQUEST);
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
    public ResponseEntity<?> updateEntry(@PathVariable ObjectId targetId, @RequestBody JournalEntry newEntry){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();
        User user = userService.findByUsername(userName);
        List<JournalEntry> collect = user.getJournalEntryList().stream().filter(x -> x.getId().equals(targetId)).collect(Collectors.toList());
        if (!collect.isEmpty()){
            Optional<JournalEntry> journalEntry =  journalEntryService.getEntryById(targetId);
            if (journalEntry.isPresent()){
                JournalEntry oldEntry = journalEntry.get();
                oldEntry.setTitle(newEntry.getTitle()!=null && !newEntry.getTitle().isEmpty() ? newEntry.getTitle():oldEntry.getTitle());
                oldEntry.setContent(newEntry.getContent()!=null && !newEntry.getContent().isEmpty() ? newEntry.getContent(): oldEntry.getContent());
                journalEntryService.saveEntry(oldEntry, userName);
                return new ResponseEntity<>(oldEntry, HttpStatus.OK);
            }
        }

        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }


}

