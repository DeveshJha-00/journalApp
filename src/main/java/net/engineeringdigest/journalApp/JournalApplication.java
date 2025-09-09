package net.engineeringdigest.journalApp;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JournalApplication {
    public static void main(String[] args) {
        SpringApplication.run(JournalApplication.class, args);
    }

    /**
     * MongoDB Transaction Manager - Only enable for replica set deployments
     * Railway MongoDB runs as single instance, so transactions are not supported
     * Uncomment the following for MongoDB Atlas or replica set deployments:
     */
    /*
    @Bean
    @ConditionalOnProperty(name = "mongodb.transactions.enabled", havingValue = "true")
    public PlatformTransactionManager transactionManager(MongoDatabaseFactory dbFactory){
        return new MongoTransactionManager(dbFactory);
    }
    */

    //PTF --> interface
    //MTM --> actual implementation of PTF

}