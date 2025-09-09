package net.engineeringdigest.journalApp.services;

import lombok.extern.slf4j.Slf4j;
import net.engineeringdigest.journalApp.entity.User;
import net.engineeringdigest.journalApp.repository.UserRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisService redisService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public void saveUser(User user){
        userRepository.save(user);
        // Invalidate user cache
        if (user.getId() != null) {
            redisService.invalidateUserCache(user.getId().toHexString());
            // Also invalidate username-based cache
            redisService.deletePattern("user:username:" + user.getUserName());
        }
    }

    public void saveNewUser(User user){
        try{
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            user.setRoles(Arrays.asList("USER"));
            userRepository.save(user);
        }catch (Exception e){
            log.error("Error saving new user: {}", e.getMessage());
//            log.warn("User with username {} already exists", user.getUserName());
//            log.info("User with username {} already exists, please choose a different username", user.getUserName());
//            log.debug("User with username {} already exists, please choose a different username", user.getUserName());
//            log.trace("User with username {} already exists, please choose a different username", user.getUserName());
//            throw new RuntimeException("Error saving new user: " + e.getMessage());
        }

    }

    public void saveAdmin(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRoles(Arrays.asList("USER","ADMIN"));
        userRepository.save(user);
    }

    public List<User> findAllUsers(){
        return userRepository.findAll();
    }

    public Optional<User> findUserById(ObjectId id){
        return userRepository.findById(id);
    }

    public void deleteUserById(ObjectId id){
        userRepository.deleteById(id);
    }

    public void deleteUserByUserName(String userName){
        User user = findByUsername(userName);
        if (user != null) {
            redisService.invalidateUserCache(user.getId().toHexString());
            // Also invalidate username-based cache
            redisService.delete("user:username:" + userName);
        }
        userRepository.deleteByUserName(userName);
    }

    public User findByUsername(String userName){
        // Try to get from cache first using manual caching
        String userCacheKey = "user:username:" + userName;
        User cachedUser = redisService.get(userCacheKey, User.class);

        if (cachedUser != null) {
            log.debug("User found in cache: {}", userName);
            return cachedUser;
        }

        // If not in cache, get from database
        User user = userRepository.findByuserName(userName);
        if (user != null) {
            // Cache the user with username key
            redisService.set(userCacheKey, user, RedisService.USER_PROFILE_TTL);

            // Also cache user profile separately for faster access by ID
            String userProfileKey = redisService.buildUserProfileKey(user.getId().toHexString());
            redisService.set(userProfileKey, user, RedisService.USER_PROFILE_TTL);

            log.debug("User cached: {}", userName);
        }
        return user;
    }


}
