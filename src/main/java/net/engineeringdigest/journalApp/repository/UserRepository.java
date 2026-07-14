package net.engineeringdigest.journalApp.repository;

import net.engineeringdigest.journalApp.entity.User;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface UserRepository extends MongoRepository<User, ObjectId> {
    //repo extending MongoRepo to provide dbms functionalities
    User findByuserName(String userName);
    void deleteByUserName(String userName);
    User findFirstByEmail(String email);

    @Query("{ 'sentimentAnalysis': true, 'email': { $nin: [null, ''] } }")
    List<User> findEligibleForBiweeklyReports();
}
