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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

    private boolean isAdministrator(User userForChangeRole) {
        return userForChangeRole.getRoles().contains(Role.ROLE_ADMINISTRATOR);
    }

    private boolean hasUserRole(User user, String role) {
        return user.getRoles().contains(Role.valueOf("ROLE_"+ role));
    }

    private String getUserGroup(Set<Role> userRoles) {
        String group = "";

        for (Role role : userRoles) {
            if (Role.ROLE_USER == role || Role.ROLE_ACCOUNTANT == role) {
                group = "business";
            } else if (Role.ROLE_ADMINISTRATOR == role) {
                group = "admin";
            }
        }

        return group;
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
            if (userRepository.count() == 0) {
                user.setRole(Set.of(Role.ROLE_ADMINISTRATOR));
            } else {
                user.setRole(Set.of(Role.ROLE_USER));
            }
            saveUser(user);
            return true;
        }
        return false;
    }

    public User checkAuth(UserDetails details) {
        return this.findUserByEmail(details.getUsername()).get();
    }

    public User changeRole(Map<String, String> req) {
        Set<String> keySet = req.keySet();

        String user = "";
        String role = "";
        String operation = "";

        for (String key : keySet) {
            if ("user".equals(key)) {
                user = req.get(key);
            } else if ("role".equals(key)) {
                role = req.get(key);
            } else if ("operation".equals(key)) {
                operation = req.get(key);
            }
        }

        if (!List.of("ADMINISTRATOR", "USER", "ACCOUNTANT").contains(role)) {
            throw new RuntimeException("Role not found!");
        }

        Optional<User> optUserForChangeRole = userRepository.findByEmailIgnoreCase(user);

        if (optUserForChangeRole.isPresent()) {
            User userForChangeRole = optUserForChangeRole.get();
            if ("GRANT".equals(operation) && (getUserGroup(userForChangeRole.getRoles()).equals("business") && role.equals("ADMINISTRATOR") ||
                    getUserGroup(userForChangeRole.getRoles()).equals("admin") && ((role.equals("USER") || role.equals("ACCOUNTANT"))))) {
                throw new RuntimeException("The user cannot combine administrative and business roles!");
            }
            if (!hasUserRole(userForChangeRole, role) && "REMOVE".equals(operation)) {
                throw new RuntimeException("The user does not have a role!");
            }
            if (userForChangeRole.getRoles().size() == 1 && "REMOVE".equals(operation)) {
                throw new RuntimeException("The user must have at least one role!");
            }
            if (isAdministrator(userForChangeRole) && "REMOVE".equals(operation)) {
                throw new RuntimeException("Can't remove ADMINISTRATOR role!");
            }

            Set<Role> roles = userForChangeRole.getRoles();
            if ("REMOVE".equals(operation)) {
                roles.remove(Role.valueOf("ROLE_" + role));
            } else if  ("GRANT".equals(operation)) {
                roles.add(Role.valueOf("ROLE_" + role));
            }
            userForChangeRole.setRole(roles);

            return saveUser(userForChangeRole);

        }

        throw new RuntimeException("User not found!");

    }


}
