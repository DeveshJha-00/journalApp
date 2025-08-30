package net.engineeringdigest.journalApp.repository;

import com.mongodb.assertions.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
public class UserRepositoryImplTests {

    @Autowired
    private UserRepositoryImpl userRepositoryImpl;

    @Test
    @Disabled("Disabled for CI/CD - requires database connection")
    public void testGetUsersForSA() {
        Assertions.assertNotNull(userRepositoryImpl.getUsersForSA());
    }

}
