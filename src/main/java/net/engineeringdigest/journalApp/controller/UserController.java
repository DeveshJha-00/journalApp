package net.engineeringdigest.journalApp.controller;

import net.engineeringdigest.journalApp.dto.AnalyticsResponseDTO;
import net.engineeringdigest.journalApp.dto.BiweeklyReportResponseDTO;
import net.engineeringdigest.journalApp.dto.DTOMapper;
import net.engineeringdigest.journalApp.entity.BiweeklyReport;
import net.engineeringdigest.journalApp.entity.User;
import net.engineeringdigest.journalApp.repository.BiweeklyReportRepository;
import net.engineeringdigest.journalApp.repository.UserRepository;
import net.engineeringdigest.journalApp.services.AnalyticsService;
import net.engineeringdigest.journalApp.services.UserService;
import net.engineeringdigest.journalApp.utils.JwtUtil;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private BiweeklyReportRepository biweeklyReportRepository;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private DTOMapper dtoMapper;

    @Autowired
    private JwtUtil jwtUtil;


    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("userName", user.getUserName());
        response.put("email", user.getEmail());
        response.put("sentimentAnalysis", user.isSentimentAnalysis());
        response.put("authProvider", user.getAuthProvider());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping() 
    public ResponseEntity<?> updateUser(@RequestBody User user){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();
        User userInDB = userService.findByUsername(userName);
        userInDB.setUserName(user.getUserName());
        userInDB.setPassword(user.getPassword());
        userService.saveNewUser(userInDB);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping("/username")
    public ResponseEntity<?> updateUsername(@RequestBody Map<String, String> body) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentName = authentication.getName();
        User user = userService.findByUsername(currentName);
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        String newUsername = body.get("userName");
        if (newUsername == null || newUsername.trim().isEmpty()) {
            return new ResponseEntity<>("'userName' is required", HttpStatus.BAD_REQUEST);
        }
        // Check if username is already taken
        User existing = userService.findByUsername(newUsername.trim());
        if (existing != null && !existing.getId().equals(user.getId())) {
            return new ResponseEntity<>("Username already taken", HttpStatus.CONFLICT);
        }
        user.setUserName(newUsername.trim());
        userService.saveUser(user);
        // Generate new JWT with updated username
        String newJwt = jwtUtil.generateToken(user.getUserName());
        Map<String, Object> response = new HashMap<>();
        response.put("userName", user.getUserName());
        response.put("token", newJwt);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/sentiment-analysis")
    public ResponseEntity<?> toggleSentimentAnalysis(@RequestBody Map<String, Boolean> body) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Boolean enabled = body.get("enabled");
        if (enabled == null) {
            return new ResponseEntity<>("'enabled' field is required", HttpStatus.BAD_REQUEST);
        }
        user.setSentimentAnalysis(enabled);
        userService.saveUser(user);
        Map<String, Object> response = new HashMap<>();
        response.put("sentimentAnalysis", user.isSentimentAnalysis());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping()
    public ResponseEntity<?> deleteUserById(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        userService.deleteUserByUserName(authentication.getName());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // ==================== Reports ====================

    @GetMapping("/reports")
    public ResponseEntity<?> getAllReports() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        List<BiweeklyReportResponseDTO> reports = biweeklyReportRepository
                .findByUserIdOrderByGeneratedAtDesc(user.getId())
                .stream()
                .map(dtoMapper::toResponseDTO)
                .collect(Collectors.toList());

        return new ResponseEntity<>(reports, HttpStatus.OK);
    }

    @GetMapping("/reports/{id}")
    public ResponseEntity<?> getReportById(@PathVariable String id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        try {
            ObjectId reportId = new ObjectId(id);
            Optional<BiweeklyReport> reportOpt = biweeklyReportRepository.findById(reportId);

            if (reportOpt.isEmpty()) {
                return new ResponseEntity<>("Report not found", HttpStatus.NOT_FOUND);
            }

            BiweeklyReport report = reportOpt.get();
            // Ownership check
            if (!report.getUserId().equals(user.getId())) {
                return new ResponseEntity<>("Access denied", HttpStatus.FORBIDDEN);
            }

            return new ResponseEntity<>(dtoMapper.toResponseDTO(report), HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>("Invalid report ID", HttpStatus.BAD_REQUEST);
        }
    }

    // ==================== Analytics ====================

    @GetMapping("/analytics")
    public ResponseEntity<?> getAnalytics(@RequestParam(defaultValue = "15d") String range) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.findByUsername(authentication.getName());
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        AnalyticsResponseDTO analytics = analyticsService.getAnalytics(user, range);
        return new ResponseEntity<>(analytics, HttpStatus.OK);
    }

}
