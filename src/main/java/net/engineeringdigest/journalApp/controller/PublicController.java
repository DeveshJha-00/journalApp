package net.engineeringdigest.journalApp.controller;

import lombok.extern.slf4j.Slf4j;
import net.engineeringdigest.journalApp.entity.User;
import net.engineeringdigest.journalApp.filter.JwtFilter;
import net.engineeringdigest.journalApp.services.UserDetailsAuthServiceImpl;
import net.engineeringdigest.journalApp.services.UserService;
import net.engineeringdigest.journalApp.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/public")
@Slf4j
public class PublicController {

    @Autowired
    private UserService userService;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private UserDetailsAuthServiceImpl userDetailsAuthService;
    @Autowired
    private JwtUtil jwtUtil;


    @GetMapping("/health-check")
    public String healthCheck(){
        return "OK";
    }

    @PostMapping("/signup")
    public void signup(@RequestBody User user) {
        userService.saveNewUser(user);
    }


    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody User user) {
        try{
            String name = user.getUserName(); String pw = user.getPassword();
            Authentication authenticate = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(name, pw));
            UserDetails userDetails = userDetailsAuthService.loadUserByUsername(user.getUserName());
            String jwt = jwtUtil.generateToken(userDetails.getUsername());
            return new ResponseEntity<>(jwt, HttpStatus.OK);
        }catch (Exception e){
            log.error("Error while creating authentication jwt token. " +
                    "Login failed for user {}: {}", user.getUserName(), e.getMessage());
            return new ResponseEntity<>("Incorrect username or password", HttpStatus.UNAUTHORIZED);
        }

    }

}
