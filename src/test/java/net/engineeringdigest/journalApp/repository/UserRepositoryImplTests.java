package net.engineeringdigest.journalApp.repository;


import net.engineeringdigest.journalApp.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ActiveProfiles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ActiveProfiles("dev")
public class UserRepositoryImplTests {

    @InjectMocks
    private  UserRepositoryImpl userRepositoryImpl;

    @Mock
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp(){
        MockitoAnnotations.initMocks(this);
    }

    @Disabled("Disabled for CI/CD - mocked test, no real DB call")
    @Test
    public void testGetUsersForSA() {
        User mockUser = User.builder()
                .userName("admin")
                .password("admin")
                .email("admin@example.com")
                .roles(new ArrayList<>())
                .sentimentAnalysis(true)
                .build();
        when(mongoTemplate.find(any(Query.class), any(Class.class)))
                .thenReturn(Arrays.asList(mockUser));

        // act
        List<User> result = userRepositoryImpl.getUsersForSA();

        // assert
        assertNotNull(result);
    }

}
