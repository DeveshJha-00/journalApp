package net.engineeringdigest.journalApp.services;

import lombok.extern.slf4j.Slf4j;
import net.engineeringdigest.journalApp.entity.Collection;
import net.engineeringdigest.journalApp.entity.JournalEntry;
import net.engineeringdigest.journalApp.entity.User;
import net.engineeringdigest.journalApp.repository.CollectionRepository;
import net.engineeringdigest.journalApp.repository.JournalEntryRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CollectionService {

    @Autowired
    private CollectionRepository collectionRepository;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private RedisService redisService;

    public List<Collection> getCollectionsByUserId(ObjectId userId) {
        return collectionRepository.findByUserId(userId);
    }

    public List<Collection> getCollectionsByUsername(String userName) {
        User user = userService.findByUsername(userName);

        // Try to get from cache first
        String userCollectionsKey = redisService.buildUserCollectionsKey(user.getId().toHexString());
        List<Collection> cachedCollections = redisService.getList(userCollectionsKey,
                new com.fasterxml.jackson.core.type.TypeReference<List<Collection>>() {});

        if (cachedCollections != null) {
            log.debug("Collections found in cache for user: {}", userName);
            return cachedCollections;
        }

        // If not in cache, get from database
        List<Collection> collections = collectionRepository.findByUserId(user.getId());

        // Cache individual collections as well
        for (Collection collection : collections) {
            String collectionKey = redisService.buildCollectionKey(collection.getId().toHexString());
            redisService.set(collectionKey, collection, RedisService.COLLECTIONS_TTL);
        }

        // Cache the user's collections list
        redisService.set(userCollectionsKey, collections, RedisService.COLLECTIONS_TTL);
        log.debug("Collections cached for user: {}", userName);

        return collections;
    }

    public Optional<Collection> getCollectionById(ObjectId id) {
        // Try to get from cache first
        String collectionKey = redisService.buildCollectionKey(id.toHexString());
        Collection cachedCollection = redisService.get(collectionKey, Collection.class);

        if (cachedCollection != null) {
            log.debug("Collection found in cache: {}", id);
            return Optional.of(cachedCollection);
        }

        // If not in cache, get from database
        Optional<Collection> collection = collectionRepository.findById(id);

        // Cache the result if found
        if (collection.isPresent()) {
            redisService.set(collectionKey, collection.get(), RedisService.COLLECTIONS_TTL);
            log.debug("Collection cached: {}", id);
        }

        return collection;
    }

    public Map<ObjectId, String> getCollectionNamesByIds(Set<ObjectId> collectionIds) {
        if (collectionIds == null || collectionIds.isEmpty()) {
            return Map.of();
        }

        List<Collection> collections = new ArrayList<>();
        collectionRepository.findAllById(collectionIds).forEach(collections::add);

        return collections.stream()
                .collect(Collectors.toMap(Collection::getId, Collection::getName, (existing, duplicate) -> existing));
    }

    // @Transactional - Disabled for Railway MongoDB (single instance, no replica set)
    public Collection saveCollection(Collection collection, String userName) {
        User user = userService.findByUsername(userName);
        collection.setUserId(user.getId());
        collection.setCreatedDate(LocalDateTime.now());
        Collection savedCollection = collectionRepository.save(collection);

        // Invalidate user's collections cache
        String userCollectionsKey = redisService.buildUserCollectionsKey(user.getId().toHexString());
        redisService.delete(userCollectionsKey);

        // Cache the saved collection
        String collectionKey = redisService.buildCollectionKey(savedCollection.getId().toHexString());
        redisService.set(collectionKey, savedCollection, RedisService.COLLECTIONS_TTL);

        log.debug("Collection saved and cached: {}", savedCollection.getId());
        return savedCollection;
    }

    @Transactional
    public boolean deleteById(ObjectId id, String userName) {
        try {
            User user = userService.findByUsername(userName);
            Optional<Collection> collection = collectionRepository.findById(id);
            if (collection.isPresent() && collection.get().getUserId().equals(user.getId())) {
                // Unassign all entries from this collection (entries are NOT deleted)
                List<JournalEntry> entries = journalEntryRepository.findByUserIdAndCollectionId(user.getId(), id);
                for (JournalEntry entry : entries) {
                    entry.setCollectionId(null);
                    journalEntryRepository.save(entry);
                    // Invalidate entry cache
                    String entryKey = redisService.buildEntryKey(entry.getId().toHexString());
                    redisService.delete(entryKey);
                }

                collectionRepository.deleteById(id);

                // Invalidate all related caches
                redisService.invalidateCollectionCache(id.toHexString());
                String collectionEntriesKey = redisService.buildCollectionEntriesKey(id.toHexString());
                redisService.delete(collectionEntriesKey);
                String userCollectionsKey = redisService.buildUserCollectionsKey(user.getId().toHexString());
                redisService.delete(userCollectionsKey);

                log.debug("Collection deleted, entries unassigned, and cache invalidated: {}", id);
                return true;
            }
        } catch (Exception e) {
            throw new RuntimeException("Error deleting collection: ", e);
        }
        return false;
    }

    public boolean isCollectionOwnedByUser(ObjectId collectionId, String userName) {
        User user = userService.findByUsername(userName);
        Optional<Collection> collection = collectionRepository.findById(collectionId);
        return collection.isPresent() && collection.get().getUserId().equals(user.getId());
    }
}
