package net.engineeringdigest.journalApp.services;
import lombok.extern.slf4j.Slf4j;
import net.engineeringdigest.journalApp.entity.JournalEntry;
import net.engineeringdigest.journalApp.entity.User;
import net.engineeringdigest.journalApp.repository.JournalEntryRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@Slf4j
public class JournalEntryService {

    @Autowired
    private JournalEntryRepository journalEntryRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private CollectionService collectionService;
    @Autowired
    private RedisService redisService;
    @Autowired
    private SentimentAnalysisService sentimentAnalysisService;


    public List<JournalEntry> getAllEntries(){
        return journalEntryRepository.findAll();
    }

    public Optional<JournalEntry> getEntryById(ObjectId id){
        // Try to get from cache first
        String entryKey = redisService.buildEntryKey(id.toHexString());
        JournalEntry cachedEntry = redisService.get(entryKey, JournalEntry.class);

        if (cachedEntry != null) {
            log.debug("Journal entry found in cache: {}", id);
            return Optional.of(cachedEntry);
        }

        // If not in cache, get from database
        Optional<JournalEntry> entry = journalEntryRepository.findById(id);

        // Cache the result if found
        if (entry.isPresent()) {
            redisService.set(entryKey, entry.get(), RedisService.ENTRIES_TTL);
            log.debug("Journal entry cached: {}", id);
        }

        return entry;
    }

    // @Transactional - Disabled for Railway MongoDB (single instance, no replica set)
    public void saveEntry(JournalEntry entry, String userName){
        User user = userService.findByUsername(userName);
        entry.setDate(LocalDateTime.now());
        JournalEntry saved = journalEntryRepository.save(entry);
        user.getJournalEntryList().add(saved);
        userService.saveUser(user);

        // Perform sentiment analysis if user has opted in
        if (user.isSentimentAnalysis()) {
            try {
                sentimentAnalysisService.analyzeSentiment(saved);
                // Save the entry again with sentiment data
                journalEntryRepository.save(saved);
                log.debug("Sentiment analysis completed for entry: {}", saved.getId());
            } catch (Exception e) {
                log.error("Failed to analyze sentiment for entry {}: {}", saved.getId(), e.getMessage());
                // Continue without sentiment analysis - don't fail the entire operation
            }
        }

        // Cache the saved entry (with sentiment data if analyzed)
        String entryKey = redisService.buildEntryKey(saved.getId().toHexString());
        redisService.set(entryKey, saved, RedisService.ENTRIES_TTL);

        // Invalidate user's recent entries cache
        String recentEntriesKey = redisService.buildUserRecentEntriesKey(user.getId().toHexString());
        redisService.delete(recentEntriesKey);

        // Invalidate collection entries cache if entry belongs to a collection
        if (saved.getCollectionId() != null) {
            String collectionEntriesKey = redisService.buildCollectionEntriesKey(saved.getCollectionId().toHexString());
            redisService.delete(collectionEntriesKey);
        }

        log.debug("Journal entry saved and cached: {}", saved.getId());
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

    public List<JournalEntry> getEntriesByCollectionId(ObjectId collectionId, String userName) {
        // Try to get from cache first
        String collectionEntriesKey = redisService.buildCollectionEntriesKey(collectionId.toHexString());
        List<JournalEntry> cachedEntries = redisService.getList(collectionEntriesKey,
                new com.fasterxml.jackson.core.type.TypeReference<List<JournalEntry>>() {});

        if (cachedEntries != null) {
            log.debug("Collection entries found in cache: {}", collectionId);
            return cachedEntries;
        }

        // If not in cache, get from database
        User user = userService.findByUsername(userName);
        List<JournalEntry> entries = user.getJournalEntryList().stream()
            .filter(entry -> collectionId.equals(entry.getCollectionId()))
            .collect(Collectors.toList());

        // Cache the result
        redisService.set(collectionEntriesKey, entries, RedisService.ENTRIES_TTL);
        log.debug("Collection entries cached: {}", collectionId);

        return entries;
    }

    @Transactional
    public boolean assignToCollection(ObjectId entryId, ObjectId collectionId, String userName) {
        try {
            User user = userService.findByUsername(userName);

            // Verify user owns the entry
            boolean ownsEntry = user.getJournalEntryList().stream()
                    .anyMatch(entry -> entry.getId().equals(entryId));

            if (!ownsEntry) {
                return false;
            }

            // Verify user owns the collection (if collectionId is not null)
            if (collectionId != null && !collectionService.isCollectionOwnedByUser(collectionId, userName)) {
                return false;
            }

            Optional<JournalEntry> entryOpt = journalEntryRepository.findById(entryId);
            if (entryOpt.isPresent()) {
                JournalEntry entry = entryOpt.get();
                entry.setCollectionId(collectionId);
                journalEntryRepository.save(entry);
                return true;
            }
        } catch (Exception e) {
            throw new RuntimeException("Error assigning entry to collection: ", e);
        }
        return false;
    }


}

//controller(routes) --> service --> repo (extends MongoD repo || CRUD op)
// C->S->R
