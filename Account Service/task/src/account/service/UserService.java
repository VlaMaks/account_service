package account.service;

import account.entity.Role;
import account.entity.Status;
import account.entity.User;
import account.exception.PasswordExceptionReason;
import account.repository.UserRepository;
import account.security.SecurityConfig;
import account.validation.UserPasswordValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class UserService {

    private final UserRepository userRepository;
    public static final int MAX_FAILED_ATTEMPTS = 5;

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

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public static boolean isAdministrator(User userForChangeRole) {
        return userForChangeRole.getRoles().contains(Role.ROLE_ADMINISTRATOR);
    }

    private boolean hasUserRole(User user, String role) {
        return user.getRoles().contains(Role.valueOf("ROLE_"+ role));
    }

    private String getUserGroup(List<Role> userRoles) {
        String group = "";

        for (Role role : userRoles) {
            if (Role.ROLE_USER == role || Role.ROLE_ACCOUNTANT == role || Role.ROLE_AUDITOR == role) {
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
            List<Role> roles = new ArrayList<>();
            if (userRepository.count() == 0) {
                roles.add(Role.ROLE_ADMINISTRATOR);
            } else {
                roles.add(Role.ROLE_USER);
            }
            user.setRole(roles);
            saveUser(user);
            return true;
        }
        return false;
    }

    public User checkAuth(UserDetails details) {
        return findUserByEmail(details.getUsername()).get();
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

        if (!List.of("ADMINISTRATOR", "USER", "ACCOUNTANT", "AUDITOR").contains(role)) {
            throw new RuntimeException("Role not found!");
        }

        Optional<User> optUserForChangeRole = userRepository.findByEmailIgnoreCase(user);

        if (optUserForChangeRole.isPresent()) {
            User userForChangeRole = optUserForChangeRole.get();
            if ("GRANT".equals(operation) && (getUserGroup(userForChangeRole.getRoles()).equals("business") && role.equals("ADMINISTRATOR") ||
                    getUserGroup(userForChangeRole.getRoles()).equals("admin") && ((role.equals("USER") || role.equals("AUDITOR") || role.equals("ACCOUNTANT"))))) {
                throw new RuntimeException("The user cannot combine administrative and business roles!");
            }
            if (!hasUserRole(userForChangeRole, role) && "REMOVE".equals(operation)) {
                throw new RuntimeException("The user does not have a role!");
            }
            if (isAdministrator(userForChangeRole) && "REMOVE".equals(operation)) {
                throw new RuntimeException("Can't remove ADMINISTRATOR role!");
            }
            if (userForChangeRole.getRoles().size() == 1 && "REMOVE".equals(operation)) {
                throw new RuntimeException("The user must have at least one role!");
            }

            List<Role> roles = userForChangeRole.getRoles();
            if ("REMOVE".equals(operation)) {
                roles.remove(Role.valueOf("ROLE_" + role));
            } else if  ("GRANT".equals(operation)) {
                if (!roles.contains(Role.valueOf("ROLE_" + role))) {
                    roles.add(Role.valueOf("ROLE_" + role));
                }
            }
            Collections.sort(roles, new Comparator<Role>() {
                @Override
                public int compare(Role o1, Role o2) {
                    return o1.toString().compareTo(o2.toString());
                }

            });
            userForChangeRole.setRole(roles);

            return saveUser(userForChangeRole);

        }

        throw new RuntimeException("User not found!");

    }

    public List<User> getUsers() {
        return userRepository.findAll(Sort.by("id"));
    }

    public Map<String, String> deleteUser(String email) {
        Optional<User> optUserForDelete = userRepository.findByEmailIgnoreCase(email);

        if (optUserForDelete.isPresent()) {
            User userForDelete = optUserForDelete.get();

            if (isAdministrator(userForDelete)) {
                throw new RuntimeException("Can't remove ADMINISTRATOR role!");
            }

            userRepository.delete(userForDelete);

            return Map.of("user", email, "status", "Deleted successfully!");
        }

        throw new RuntimeException("User not found!");
    }

    @Transactional
    public void increaseFailedAttempts(User user) {
        int newFailAttempts = user.getFailedAttempt() + 1;
        userRepository.updateFailedAttempts(newFailAttempts, user.getEmail());
    }

    @Transactional
    public void resetFailedAttempts(String email) {
        userRepository.updateFailedAttempts(0, email);
    }

    public void lock(User user) {
        user.setStatus(Status.BANNED);
        user.setLockTime(LocalDateTime.now());
        saveUser(user);
    }

    @Transactional
    public User setUserStatus(Map<String, String> req) {
        Set<String> keySet = req.keySet();

        String user = "";
        String operation = "";

        for (String key : keySet) {
            if ("user".equals(key)) {
                user = req.get(key);
            } else if ("operation".equals(key)) {
                operation = req.get(key);
            }
        }

        if (!List.of("LOCK", "UNLOCK").contains(operation)) {
            throw new RuntimeException("Operation not found!");
        }

        Optional<User> optUserForChangeRole = userRepository.findByEmailIgnoreCase(user);

        if (optUserForChangeRole.isPresent()) {
            User userForChangeStatus = optUserForChangeRole.get();
            if (isAdministrator(userForChangeStatus) && "LOCK".equals(operation)) {
                throw new RuntimeException("Can't lock the ADMINISTRATOR!");
            }
            if ("LOCK".equals(operation)) {
                userForChangeStatus.setStatus(Status.BANNED);
            } else if("UNLOCK".equals(operation)) {
                userForChangeStatus.setStatus(Status.ACTIVE);
                resetFailedAttempts(userForChangeStatus.getEmail());
            }
            return saveUser(userForChangeStatus);
        }
        throw new RuntimeException("User not found!");
    }
}
