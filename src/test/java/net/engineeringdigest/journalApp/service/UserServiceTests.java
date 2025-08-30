package net.engineeringdigest.journalApp.service;

import net.engineeringdigest.journalApp.config.TestMailConfig;
import net.engineeringdigest.journalApp.entity.User;
import net.engineeringdigest.journalApp.repository.UserRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;


//JUNIT TESTING
@SpringBootTest
@Import(TestMailConfig.class)
@ActiveProfiles("dev")
//@Disabled("Disabled for CI/CD - requires database connection")
public class UserServiceTests {

    @Autowired
    private UserRepository userRepository;

    @Test
    public void testFindByUsername() {
        assertNotNull(userRepository.findByuserName("admin"));
    }


}
