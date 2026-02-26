package net.engineeringdigest.journalApp.repository;

import net.engineeringdigest.journalApp.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRepositoryImplTests {

    @InjectMocks
    private UserRepositoryImpl userRepositoryImpl;

    @Mock
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void setUp() {
        // No manual init needed when using MockitoExtension
    }

    @Test
    void testGetUsersForSA() {

        User mockUser = User.builder()
                .userName("admin")
                .password("admin")
                .email("admin@example.com")
                .roles(List.of())
                .sentimentAnalysis(true)
                .build();

        when(mongoTemplate.find(any(Query.class), any(Class.class)))
                .thenReturn(Arrays.asList(mockUser));

        List<User> result = userRepositoryImpl.getUsersForSA();

        assertEquals(1, result.size());
        assertEquals("admin", result.get(0).getUserName());
    }
}