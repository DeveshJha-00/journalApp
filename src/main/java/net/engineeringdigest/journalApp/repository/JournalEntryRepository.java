package net.engineeringdigest.journalApp.repository;
import net.engineeringdigest.journalApp.entity.JournalEntry;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;


public interface JournalEntryRepository extends MongoRepository<JournalEntry, ObjectId> {
    //repo extending MongoRepo to provide dbms functionalities
    List<JournalEntry> findByCollectionId(ObjectId collectionId);
}
