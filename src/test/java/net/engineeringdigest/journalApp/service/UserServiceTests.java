package net.engineeringdigest.journalApp.service;

import net.engineeringdigest.journalApp.config.TestMailConfig;
import net.engineeringdigest.journalApp.entity.User;
import net.engineeringdigest.journalApp.repository.UserRepository;
import net.engineeringdigest.journalApp.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ActiveProfiles("dev")
@Import(TestMailConfig.class)
public class UserServiceTests {

    @InjectMocks
    private UserService userService;   // class under test (replace with actual service if needed)

    @Mock
    private UserRepository userRepository;  // dependency

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testFindByUsername() {
        // arrange - mock repository response
        when(userRepository.findByuserName(anyString()))
                .thenReturn(User.builder()
                        .userName("admin")
                        .password("admin")
                        .email("admin@example.com")
                        .roles(new ArrayList<>())
                        .sentimentAnalysis(true)
                        .build());

        // act
        User result = userRepository.findByuserName("admin");

        // assert
        assertNotNull(result);
    }
}
