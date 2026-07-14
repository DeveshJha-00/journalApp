package net.engineeringdigest.journalApp.service;

import net.engineeringdigest.journalApp.entity.User;
import net.engineeringdigest.journalApp.repository.UserRepository;
import net.engineeringdigest.journalApp.services.RedisService;
import net.engineeringdigest.journalApp.services.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RedisService redisService;

    @InjectMocks
    private UserService userService;

    @Test
    void findByUsername_shouldReturnUser() {

        User mockUser = User.builder()
                .userName("admin")
                .password("admin")
                .email("admin@example.com")
                .roles(new ArrayList<>())
                .sentimentAnalysis(true)
                .build();

        when(userRepository.findByuserName("admin"))
                .thenReturn(mockUser);

        User result = userService.findByUsername("admin");

        assertEquals("admin", result.getUserName());
    }
}
