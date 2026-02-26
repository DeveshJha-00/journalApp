package net.engineeringdigest.journalApp.service;

import net.engineeringdigest.journalApp.entity.User;
import net.engineeringdigest.journalApp.repository.UserRepository;
import net.engineeringdigest.journalApp.services.UserDetailsAuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsAuthServiceImplTests {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsAuthServiceImpl userDetailsAuthService;

    @Test
    void loadUserByUsername_shouldReturnUserDetails() {

        when(userRepository.findByuserName("admin"))
                .thenReturn(
                        User.builder()
                                .userName("admin")
                                .password("admin")
                                .roles(new ArrayList<>())
                                .build()
                );

        UserDetails userDetails =
                userDetailsAuthService.loadUserByUsername("admin");

        assertNotNull(userDetails);
    }
}