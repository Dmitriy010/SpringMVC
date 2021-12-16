package com.springmvc.services;

import com.springmvc.models.Role;
import com.springmvc.models.User;
import com.springmvc.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MailSender mailSender;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User userFromDb = userRepository.findByUsername(username);
        if(userFromDb ==null){
            throw new UsernameNotFoundException("User not found");
        }

        return userFromDb;

    }


    public boolean registrationuser(User user){
        User userFromDb = userRepository.findByUsername(user.getUsername());
        if(userFromDb!=null){
            return false;
        }

        user.setActive(true);
        user.setRoles(Collections.singleton(Role.USER));
        user.setActivationCode(UUID.randomUUID().toString());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);


        sendMessage(user);

        return true;
    }

    private void sendMessage(User user) {
        if(!ObjectUtils.isEmpty(user.getEmail())){
            String message = String.format(
                    "Hello, %s! \n" +
                            "Welcome to WebApp. Please, visit next link: http://localhost:8080/activate/%s",
                    user.getUsername(),
                    user.getActivationCode()
            );
            mailSender.send(user.getEmail(),"Activate code", message);
        }
    }

    public boolean activateUser(String code) {
        User user =  userRepository.findByActivationCode(code);
        if (user == null) {
            return false;
        }
        user.setActivationCode(null);
        userRepository.save(user);

        return true;
    }

    public List<User> findAll() {

        return userRepository.findAll();
    }

    public void saveUser(User user, String username, Map<String, String> form) {

        user.setUsername(username);

        Set<String> roles = Arrays.stream(Role.values())
                .map(Role::name)
                .collect(Collectors.toSet());

        user.getRoles().clear();

        for (String key : form.keySet()) {
            if (roles.contains(key)) {
                user.getRoles().add(Role.valueOf(key));
            }
        }

        userRepository.save(user);
    }

    public void updateProfile(User user, String password, String email) {

        String userEmail = user.getEmail();

        boolean isEmailChanged = (email !=null && !email.equals(userEmail)) ||
                (userEmail !=null && !userEmail.equals(email));

        if (isEmailChanged) {
            user.setEmail(email);

            if (!ObjectUtils.isEmpty(email)) {
                user.setActivationCode(UUID.randomUUID().toString());
            }
        }
        if(!ObjectUtils.isEmpty(password))
            user.setPassword(passwordEncoder.encode(password));


        userRepository.save(user);

        if (isEmailChanged)
            sendMessage(user);


    }

    public void subscribe(User currentUser, User user) {
        user.getSubscribers().add(currentUser);
        userRepository.save(user);
    }

    public void unsubscribe(User currentUser, User user) {
        user.getSubscribers().remove(currentUser);
        userRepository.save(user);
    }
}
