package net.engineeringdigest.journalApp.repository;

import net.engineeringdigest.journalApp.entity.Collection;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CollectionRepository extends MongoRepository<Collection, ObjectId> {
    //repo extending MongoRepo to provide dbms functionalities
    List<Collection> findByUserId(ObjectId userId);
}