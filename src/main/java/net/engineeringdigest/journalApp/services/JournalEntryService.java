package net.engineeringdigest.journalApp.services;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import net.engineeringdigest.journalApp.entity.JournalEntry;
import net.engineeringdigest.journalApp.entity.User;
import net.engineeringdigest.journalApp.repository.JournalEntryRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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

    public List<JournalEntry> getAllEntries() {
        return journalEntryRepository.findAll();
    }

    public Optional<JournalEntry> getEntryById(ObjectId id) {
        String entryKey = redisService.buildEntryKey(id.toHexString());
        JournalEntry cachedEntry = redisService.get(entryKey, JournalEntry.class);
        if (cachedEntry != null) {
            log.debug("Journal entry found in cache: {}", id);
            return Optional.of(cachedEntry);
        }

        Optional<JournalEntry> entry = journalEntryRepository.findById(id);
        entry.ifPresent(value -> {
            redisService.set(entryKey, value, RedisService.ENTRIES_TTL);
            log.debug("Journal entry cached: {}", id);
        });

        return entry;
    }

    public Optional<JournalEntry> getEntryByIdForUser(ObjectId id, String userName) {
        User user = userService.findByUsername(userName);
        if (user == null || user.getId() == null) {
            return Optional.empty();
        }

        Optional<JournalEntry> ownedEntry = journalEntryRepository.findByIdAndUserId(id, user.getId());
        if (ownedEntry.isPresent()) {
            redisService.set(redisService.buildEntryKey(id.toHexString()), ownedEntry.get(), RedisService.ENTRIES_TTL);
            return ownedEntry;
        }

        return getLegacyOwnedEntry(user, id);
    }

    public List<JournalEntry> getEntriesByUserName(String userName) {
        User user = userService.findByUsername(userName);
        if (user == null || user.getId() == null) {
            return List.of();
        }
        return getEntriesForUser(user);
    }

    public List<JournalEntry> getEntriesForUser(User user) {
        List<JournalEntry> entries = journalEntryRepository.findByUserIdOrderByDateDesc(user.getId());
        List<JournalEntry> legacyEntries = backfillLegacyEntries(user);
        if (!legacyEntries.isEmpty() && legacyEntries.size() > entries.size()) {
            entries = journalEntryRepository.findByUserIdOrderByDateDesc(user.getId());
        }

        if (!entries.isEmpty()) {
            return entries;
        }

        return legacyEntries.stream()
                .sorted(Comparator.comparing(
                        JournalEntry::getDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    // @Transactional - Disabled for Railway MongoDB (single instance, no replica set)
    public void createEntry(JournalEntry entry, String userName) {
        User user = userService.findByUsername(userName);
        entry.setDate(LocalDateTime.now());
        entry.setUserId(user.getId());
        JournalEntry saved = journalEntryRepository.save(entry);

        if (user.getJournalEntryList() == null) {
            user.setJournalEntryList(new ArrayList<>());
        }
        user.getJournalEntryList().add(saved);
        userService.saveUser(user);

        analyzeAndCache(saved, user);
    }

    public void updateEntry(JournalEntry entry, String userName) {
        User user = userService.findByUsername(userName);
        entry.setDate(LocalDateTime.now());
        entry.setUserId(user.getId());
        JournalEntry saved = journalEntryRepository.save(entry);

        analyzeAndCache(saved, user);
    }

    private void analyzeAndCache(JournalEntry saved, User user) {
        if (user.isSentimentAnalysis()) {
            try {
                sentimentAnalysisService.analyzeSentiment(saved);
                journalEntryRepository.save(saved);
                log.debug("Sentiment analysis completed for entry: {}", saved.getId());
            } catch (Exception e) {
                log.error("Failed to analyze sentiment for entry {}: {}", saved.getId(), e.getMessage());
            }
        }

        redisService.set(redisService.buildEntryKey(saved.getId().toHexString()), saved, RedisService.ENTRIES_TTL);
        invalidateEntryCaches(saved, user);

        log.debug("Journal entry saved and cached: {}", saved.getId());
    }

    @Transactional
    public boolean deleteById(ObjectId id, String userName) {
        try {
            User user = userService.findByUsername(userName);
            Optional<JournalEntry> entryOpt = getEntryByIdForUser(id, userName);
            if (entryOpt.isEmpty()) {
                return false;
            }

            journalEntryRepository.deleteById(id);
            redisService.delete(redisService.buildEntryKey(id.toHexString()));
            removeLegacyUserReference(user, id);
            invalidateEntryCaches(entryOpt.get(), user);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Error deleting entry: ", e);
        }
    }

    public List<JournalEntry> getEntriesByCollectionId(ObjectId collectionId, String userName) {
        User user = userService.findByUsername(userName);
        if (user == null || user.getId() == null) {
            return List.of();
        }

        String collectionEntriesKey = buildUserCollectionEntriesKey(user.getId(), collectionId);
        List<JournalEntry> cachedEntries = redisService.getList(collectionEntriesKey,
                new TypeReference<List<JournalEntry>>() {});
        if (cachedEntries != null) {
            log.debug("Collection entries found in cache: {}", collectionId);
            return cachedEntries;
        }

        List<JournalEntry> entries = journalEntryRepository.findByUserIdAndCollectionId(user.getId(), collectionId);
        if (entries.isEmpty()) {
            entries = backfillLegacyEntries(user).stream()
                    .filter(entry -> collectionId.equals(entry.getCollectionId()))
                    .collect(Collectors.toList());
        }

        redisService.set(collectionEntriesKey, entries, RedisService.ENTRIES_TTL);
        log.debug("Collection entries cached: {}", collectionId);

        return entries;
    }

    @Transactional
    public boolean assignToCollection(ObjectId entryId, ObjectId collectionId, String userName) {
        try {
            User user = userService.findByUsername(userName);
            Optional<JournalEntry> entryOpt = journalEntryRepository.findByIdAndUserId(entryId, user.getId());
            if (entryOpt.isEmpty()) {
                entryOpt = getLegacyOwnedEntry(user, entryId);
            }
            if (entryOpt.isEmpty()) {
                return false;
            }

            if (collectionId != null && !collectionService.isCollectionOwnedByUser(collectionId, userName)) {
                return false;
            }

            JournalEntry entry = entryOpt.get();
            ObjectId oldCollectionId = entry.getCollectionId();
            entry.setUserId(user.getId());
            entry.setCollectionId(collectionId);
            journalEntryRepository.save(entry);

            redisService.delete(redisService.buildEntryKey(entryId.toHexString()));
            redisService.delete(redisService.buildUserRecentEntriesKey(user.getId().toHexString()));
            redisService.deletePattern("analytics:" + user.getId().toHexString() + ":*");

            if (oldCollectionId != null) {
                redisService.delete(buildUserCollectionEntriesKey(user.getId(), oldCollectionId));
            }
            if (collectionId != null) {
                redisService.delete(buildUserCollectionEntriesKey(user.getId(), collectionId));
            }

            log.debug("Entry {} assigned to collection {}, caches invalidated", entryId, collectionId);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Error assigning entry to collection: ", e);
        }
    }

    private Optional<JournalEntry> getLegacyOwnedEntry(User user, ObjectId entryId) {
        if (user.getJournalEntryList() == null) {
            return Optional.empty();
        }

        Optional<JournalEntry> legacyEntry = user.getJournalEntryList().stream()
                .filter(entry -> entry.getId() != null && entry.getId().equals(entryId))
                .findFirst();

        legacyEntry.ifPresent(entry -> {
            if (entry.getUserId() == null) {
                entry.setUserId(user.getId());
                journalEntryRepository.save(entry);
            }
        });

        return legacyEntry;
    }

    private List<JournalEntry> backfillLegacyEntries(User user) {
        if (user.getJournalEntryList() == null || user.getJournalEntryList().isEmpty()) {
            return List.of();
        }

        List<JournalEntry> entries = new ArrayList<>(user.getJournalEntryList());
        List<JournalEntry> entriesToBackfill = entries.stream()
                .filter(entry -> entry.getUserId() == null)
                .peek(entry -> entry.setUserId(user.getId()))
                .collect(Collectors.toList());

        if (!entriesToBackfill.isEmpty()) {
            journalEntryRepository.saveAll(entriesToBackfill);
            log.info("Backfilled userId on {} legacy journal entries for user: {}",
                    entriesToBackfill.size(), user.getUserName());
        }

        return entries;
    }

    private void removeLegacyUserReference(User user, ObjectId entryId) {
        if (user.getJournalEntryList() == null) {
            return;
        }

        boolean removed = user.getJournalEntryList().removeIf(entry -> entry.getId() != null && entry.getId().equals(entryId));
        if (removed) {
            userService.saveUser(user);
        }
    }

    private void invalidateEntryCaches(JournalEntry entry, User user) {
        redisService.delete(redisService.buildUserRecentEntriesKey(user.getId().toHexString()));
        redisService.deletePattern("analytics:" + user.getId().toHexString() + ":*");

        if (entry.getCollectionId() != null) {
            redisService.delete(buildUserCollectionEntriesKey(user.getId(), entry.getCollectionId()));
        }
    }

    private String buildUserCollectionEntriesKey(ObjectId userId, ObjectId collectionId) {
        return redisService.buildCollectionEntriesKey(userId.toHexString() + ":" + collectionId.toHexString());
    }
}
