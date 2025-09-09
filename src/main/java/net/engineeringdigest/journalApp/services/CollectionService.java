package net.engineeringdigest.journalApp.services;

import lombok.extern.slf4j.Slf4j;
import net.engineeringdigest.journalApp.entity.Collection;
import net.engineeringdigest.journalApp.entity.User;
import net.engineeringdigest.journalApp.repository.CollectionRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class CollectionService {

    @Autowired
    private CollectionRepository collectionRepository;

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

    @Transactional
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
                collectionRepository.deleteById(id);

                // Invalidate all related caches
                redisService.invalidateCollectionCache(id.toHexString());
                String userCollectionsKey = redisService.buildUserCollectionsKey(user.getId().toHexString());
                redisService.delete(userCollectionsKey);

                log.debug("Collection deleted and cache invalidated: {}", id);
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