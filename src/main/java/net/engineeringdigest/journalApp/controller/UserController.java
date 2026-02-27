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
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
