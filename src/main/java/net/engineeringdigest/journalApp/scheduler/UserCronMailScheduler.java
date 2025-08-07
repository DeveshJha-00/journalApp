package net.engineeringdigest.journalApp.scheduler;

import net.engineeringdigest.journalApp.entity.JournalEntry;
import net.engineeringdigest.journalApp.entity.User;
import net.engineeringdigest.journalApp.services.SentimentAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import net.engineeringdigest.journalApp.services.EmailService;
import net.engineeringdigest.journalApp.repository.UserRepositoryImpl;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

public class UserCronMailScheduler {

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserRepositoryImpl userRepository;

    @Autowired
    private SentimentAnalysisService sentimentAnalysisService;


    @Scheduled(cron = "0 0 9 * * SUN")
    public void getUsersAndSendSAMail(){
        List<User> users = userRepository.getUsersForSA();
        for (User user : users) {
            List<JournalEntry> entries = user.getJournalEntryList();
            List<String> filteredEntries = entries.stream()
                    .filter(x -> x.getDate().isAfter(LocalDateTime.now().minus(7, ChronoUnit.DAYS)))
                    .map(x -> x.getContent())
                    .collect(Collectors.toList());
            String entry = String.join(" ", filteredEntries);
            String sentiment = sentimentAnalysisService.getSentiment(entry);
            emailService.sendEmail(user.getEmail(), "Your Weekly Journal Summary", sentiment);
        }
    }

}
