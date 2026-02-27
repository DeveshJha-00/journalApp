package net.engineeringdigest.journalApp.repository;

import net.engineeringdigest.journalApp.entity.User;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, ObjectId> {
    //repo extending MongoRepo to provide dbms functionalities
    User findByuserName(String userName);
    void deleteByUserName(String userName);
    User findByEmail(String email);
}
