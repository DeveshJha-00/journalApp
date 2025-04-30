package net.engineeringdigest.journalApp.services;
import net.engineeringdigest.journalApp.entity.JournalEntry;
import net.engineeringdigest.journalApp.entity.User;
import net.engineeringdigest.journalApp.repository.JournalEntryRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Component
public class JournalEntryService {

    @Autowired
    private JournalEntryRepository journalEntryRepository;
    @Autowired
    private UserService userService;


    public List<JournalEntry> getAllEntries(){
        return journalEntryRepository.findAll();
    }

    public Optional<JournalEntry> getEntryById(ObjectId id){
        return journalEntryRepository.findById(id);
    }

    @Transactional
    public void saveEntry(JournalEntry entry, String userName){
        User user = userService.findByUsername(userName);
        entry.setDate(LocalDateTime.now());
        JournalEntry saved = journalEntryRepository.save(entry);
        user.getJournalEntryList().add(saved);
        userService.saveUser(user);
    }

    public void saveEntry(JournalEntry entry){
        journalEntryRepository.save(entry);
    }

    @Transactional
    public boolean deleteById(ObjectId id, String userName){
        boolean removed = false;
        try {
            User user = userService.findByUsername(userName);
            removed = user.getJournalEntryList().removeIf(x -> x.getId().equals(id));
            if (removed){
                userService.saveUser(user);
                journalEntryRepository.deleteById(id);
            }
        }catch (Exception e){
            System.out.println(e);
            throw new RuntimeException("Error deleting entry : ", e);
        }
        return removed;
    }

//    public List<JournalEntry> findByUserName(String userName){}

}

//controller(routes) --> service --> repo (extends MongoD repo || CRUD op)
// C->S->R
