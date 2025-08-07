package net.engineeringdigest.journalApp.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender javaMailSender;

    public void sendEmail(String receiver, String subject, String body) {
        try{
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(receiver);
            mail.setSubject(subject);
            mail.setText(body);
            javaMailSender.send(mail);
        }catch (Exception e){
            log.error("Error sending email to {}: {}", receiver, e.getMessage());
        }
    }

}
