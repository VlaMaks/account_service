package account.service;

import account.entity.Role;
import account.entity.Status;
import account.entity.User;
import account.exception.PasswordExceptionReason;
import account.repository.UserRepository;
import account.security.SecurityConfig;
import account.validation.UserPasswordValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email);
    }

    public boolean changePassword(Map<String, String> bd, UserDetails details) {
        User userForChangePassword = null;

        for (Map.Entry<String, String> entry : bd.entrySet()) {
            if (!"new_password".equals(entry.getKey())) {
                throw new RuntimeException("incorrect name of parameter");
            }

            userForChangePassword = findUserByEmail(details.getUsername()).get();
            String newPassword = entry.getValue();

            UserPasswordValidator.ValidationResult result = UserPasswordValidator
                    .isLengthValid()
                    .and(UserPasswordValidator.isNotCompr())
                    .and(UserPasswordValidator.isUniq(userForChangePassword, SecurityConfig.getEncoder()))
                    .apply(newPassword);

            if (result != UserPasswordValidator.ValidationResult.SUCCESS) {
                throw new RuntimeException(PasswordExceptionReason.getPasswordExceptionReason(result));
            }

            userForChangePassword.setPassword(SecurityConfig.getEncoder().encode(newPassword));
            this.saveUser(userForChangePassword);
        }
        return true;
    }

    private User saveUser(User user) {
        return userRepository.save(user);
    }

    public boolean signupUser(User user) {
        Optional<User> us = findUserByEmail(user.getEmail());

        if (!us.isPresent()) {
            String userPassword = user.getPassword();
            UserPasswordValidator.ValidationResult result = UserPasswordValidator
                    .isLengthValid()
                    .and(UserPasswordValidator.isNotCompr())
                    .apply(userPassword);

            if (result != UserPasswordValidator.ValidationResult.SUCCESS) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, PasswordExceptionReason.getPasswordExceptionReason(result));
            }

            user.setEmail(user.getEmail().toLowerCase());
            user.setPassword(SecurityConfig.getEncoder().encode(userPassword));
            user.setStatus(Status.ACTIVE);
            user.setRole(Role.USER);
            this.saveUser(user);
            return true;
        }
        return false;
    }

    public User checkAuth(UserDetails details) {
        return this.findUserByEmail(details.getUsername()).get();
    }
}
