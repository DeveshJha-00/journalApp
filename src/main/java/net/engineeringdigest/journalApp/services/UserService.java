package net.engineeringdigest.journalApp.services;

import net.engineeringdigest.journalApp.entity.User;
import net.engineeringdigest.journalApp.repository.UserRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class UserService {

    @Autowired
    private UserRepository userRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public void saveUser(User user){
        userRepository.save(user);
    }

    public void saveNewUser(User user){
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRoles(Arrays.asList("USER"));
        userRepository.save(user);
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
        userRepository.deleteByUserName(userName);
    }

    public User findByUsername(String userName){
        return userRepository.findByuserName(userName);
    }


}
