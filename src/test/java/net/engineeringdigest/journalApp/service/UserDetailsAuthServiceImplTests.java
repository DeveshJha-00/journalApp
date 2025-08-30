package net.engineeringdigest.journalApp.service;

import net.engineeringdigest.journalApp.entity.User;
import net.engineeringdigest.journalApp.repository.UserRepository;
import net.engineeringdigest.journalApp.services.UserDetailsAuthServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;

import static org.mockito.Mockito.when;

@ActiveProfiles("dev")
public class UserDetailsAuthServiceImplTests {

    @InjectMocks
    private UserDetailsAuthServiceImpl userDetailsAuthService;

    @Mock
    private UserRepository userRepository;

    @BeforeEach
    void setUp(){
        MockitoAnnotations.initMocks(this);
    }

    @Disabled
    @Test
    void loadUserByUsernameTest(){
        when(userRepository.findByuserName(ArgumentMatchers.anyString()))
                .thenReturn(User.builder().userName("admin").password("admin").roles(new ArrayList<>()).build());
        UserDetails user = userDetailsAuthService.loadUserByUsername("admin");
        Assertions.assertNotNull(user);
    }

}
