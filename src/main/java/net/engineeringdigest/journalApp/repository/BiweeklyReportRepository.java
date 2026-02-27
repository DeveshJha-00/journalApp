package net.engineeringdigest.journalApp.repository;

import net.engineeringdigest.journalApp.entity.BiweeklyReport;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface BiweeklyReportRepository extends MongoRepository<BiweeklyReport, ObjectId> {

    List<BiweeklyReport> findByUserIdOrderByGeneratedAtDesc(ObjectId userId);
}
