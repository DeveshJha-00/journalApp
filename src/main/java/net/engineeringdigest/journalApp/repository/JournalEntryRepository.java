package net.engineeringdigest.journalApp.repository;
import net.engineeringdigest.journalApp.entity.JournalEntry;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public interface JournalEntryRepository extends MongoRepository<JournalEntry, ObjectId> {
    //repo extending MongoRepo to provide dbms functionalities
    List<JournalEntry> findByUserId(ObjectId userId);
    List<JournalEntry> findByUserIdOrderByDateAsc(ObjectId userId);
    List<JournalEntry> findByUserIdOrderByDateDesc(ObjectId userId);
    List<JournalEntry> findByUserIdAndDateAfterOrderByDateAsc(ObjectId userId, LocalDateTime date);
    List<JournalEntry> findByUserIdAndDateAfterOrderByDateDesc(ObjectId userId, LocalDateTime date);
    List<JournalEntry> findByUserIdAndCollectionId(ObjectId userId, ObjectId collectionId);
    Optional<JournalEntry> findByIdAndUserId(ObjectId id, ObjectId userId);
    boolean existsByIdAndUserId(ObjectId id, ObjectId userId);
}
