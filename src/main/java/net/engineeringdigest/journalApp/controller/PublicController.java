package net.engineeringdigest.journalApp.controller;

import lombok.extern.slf4j.Slf4j;
import net.engineeringdigest.journalApp.config.AuthCookieService;
import net.engineeringdigest.journalApp.entity.User;
import net.engineeringdigest.journalApp.filter.JwtFilter;
import net.engineeringdigest.journalApp.services.UserDetailsAuthServiceImpl;
import net.engineeringdigest.journalApp.services.UserService;
import net.engineeringdigest.journalApp.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

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
    @Autowired
    private AuthCookieService authCookieService;


    @GetMapping("/health-check")
    public String healthCheck(){
        return "OK";
    }

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody User user) {
        try {
            userService.saveNewUser(user);
            return new ResponseEntity<>("User created", HttpStatus.CREATED);
        } catch (DuplicateKeyException e) {
            log.warn("Signup rejected because username already exists: {}", user.getUserName());
            return new ResponseEntity<>("Username already exists", HttpStatus.CONFLICT);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }


    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody User user) {
        try{
            String name = user.getUserName(); String pw = user.getPassword();
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(name, pw));
            UserDetails userDetails = userDetailsAuthService.loadUserByUsername(name);
            String jwt = jwtUtil.generateToken(userDetails.getUsername());
            return new ResponseEntity<>(jwt, HttpStatus.OK);
        }catch (Exception e){
            log.error("Error while creating authentication jwt token. " +
                    "Login failed for user {}: {}", user.getUserName(), e.getMessage());
            return new ResponseEntity<>("Incorrect username or password", HttpStatus.UNAUTHORIZED);
        }

    }

    @PostMapping("/logout-cookie")
    public ResponseEntity<Void> clearOAuthCookie(HttpServletResponse response) {
        authCookieService.clearJwtCookie(response);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }



}
