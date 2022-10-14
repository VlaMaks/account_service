package account.controller;

import account.entity.EmpSal;
import account.entity.SecurityEvent;
import account.entity.SecurityEventEnum;
import account.entity.User;
import account.service.EmplService;
import account.service.SecurityEventService;
import account.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import javax.validation.Valid;
import java.util.*;

@RestController
public class MainController {

    private final UserService userService;
    private final EmplService emplService;
    private final SecurityEventService securityEventService;
    public MainController(UserService userService, EmplService emplService, SecurityEventService securityEventService) {
        this.userService = userService;
        this.emplService = emplService;
        this.securityEventService = securityEventService;
    }
    @DeleteMapping("/api/admin/user/{email}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable String email, @AuthenticationPrincipal UserDetails details) {
        ResponseEntity<Map<String, String>> responseEntity = null;
        try {
            responseEntity = new ResponseEntity<>(userService.deleteUser(email), HttpStatus.OK);
        } catch (Exception ex) {
            HttpStatus status = null;
            if (ex.getMessage().contains("not found")) {
                status = HttpStatus.NOT_FOUND;
            } else {
                status = HttpStatus.BAD_REQUEST;
            }
            throw new ResponseStatusException(status, ex.getMessage());
        }
        securityEventService.saveEvent(SecurityEventEnum.DELETE_USER, details.getUsername(), email, "/api/admin/user");
        return responseEntity;
    }
    @PostMapping("/api/auth/signup")
    public ResponseEntity<Object> signupUser(@Valid @RequestBody User user) {
        System.out.println(user);

        if (userService.signupUser(user)) {
            securityEventService.saveEvent(SecurityEventEnum.CREATE_USER, "", user.getEmail(), "/api/auth/signup");
            return new ResponseEntity(user, HttpStatus.OK);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User exist!");
        }

    }
    @PostMapping("/api/auth/changepass")
    public ResponseEntity<Map<String, String>> changePassword(@RequestBody Map<String, String> bd, @AuthenticationPrincipal UserDetails details) {
        if (bd.size() != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "incorrect number of parameters");
        }

        ResponseEntity responseEntity = null;
        try {
            if (userService.changePassword(bd, details)) {
                securityEventService.saveEvent(SecurityEventEnum.CHANGE_PASSWORD, details.getUsername(), details.getUsername(), "/api/auth/changepass");
                responseEntity = new ResponseEntity<>(Map.of("email", details.getUsername() ,  "status", "The password has been updated successfully"), HttpStatus.OK);
            }
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
        return responseEntity;
    }
    @PostMapping("/api/acct/payments")
    public ResponseEntity<Map<String, String>> uploadPayrolls(@RequestBody List<EmpSal> payrolls) {
        try {
            emplService.uploadPayrolls(payrolls, userService);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
        return new ResponseEntity<>(Map.of("status", "Added successfully!"), HttpStatus.OK);
    }
    @PutMapping("/api/acct/payments")
    public ResponseEntity<Map<String, String>> changeUserSalary(@RequestBody EmpSal empSal) {
        try {
            emplService.changeUserSalary(empSal, userService);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
        return new ResponseEntity<>(Map.of("status", "Updated successfully!") ,HttpStatus.OK);
    }
    @PutMapping("/api/admin/user/role")
    public ResponseEntity<User> changeRole(@RequestBody Map<String, String> req, @AuthenticationPrincipal UserDetails details) {
        ResponseEntity<User> responseEntity = null;
        try {
            responseEntity = new ResponseEntity<>(userService.changeRole(req), HttpStatus.OK);
        } catch (Exception ex) {
            HttpStatus status = null;
            if (ex.getMessage().contains("not found")) {
                status = HttpStatus.NOT_FOUND;
            } else {
                status = HttpStatus.BAD_REQUEST;
            }
            throw new ResponseStatusException(status, ex.getMessage());
        }
        securityEventService.saveEvent(SecurityEventEnum.valueOf(req.get("operation") + "_ROLE"), details.getUsername(), securityEventService.getObject(SecurityEventEnum.valueOf(req.get("operation") + "_ROLE"), req.get("role"), req.get("user").toLowerCase()), "/api/admin/user/role");
        return responseEntity;
    }
    @PutMapping("/api/admin/user/access")
    public ResponseEntity<Map<String, String>> setUserStatus(@RequestBody Map<String, String> req, @AuthenticationPrincipal UserDetails details) {
        try {
            userService.setUserStatus(req);
        } catch (Exception ex) {
            HttpStatus status = null;
            if (ex.getMessage().contains("not found")) {
                status = HttpStatus.NOT_FOUND;
            } else {
                status = HttpStatus.BAD_REQUEST;
            }
            throw new ResponseStatusException(status, ex.getMessage());
        }
        securityEventService.saveEvent(SecurityEventEnum.valueOf(req.get("operation") + "_USER"), details.getUsername(), securityEventService.getObject(SecurityEventEnum.valueOf(req.get("operation") + "_USER"), "", req.get("user")), "/api/admin/user/access");
        return new ResponseEntity<>(Map.of("status", String.format("User %s %sed!", req.get("user").toLowerCase(), req.get("operation").toLowerCase())), HttpStatus.OK);
    }
    @GetMapping("/api/auth/changepass")
    public ResponseEntity<User> checkAuth(@AuthenticationPrincipal UserDetails details) {
        System.out.print("checkAuth");
        System.out.println(details.getUsername());
        return new ResponseEntity<>(userService.checkAuth(details), HttpStatus.OK);
    }
    @GetMapping("/api/empl/payment")
    public ResponseEntity<Object> getPayrolls(@AuthenticationPrincipal UserDetails details, @RequestParam(required = false) String period) {
        ResponseEntity<Object> responseEntity = null;
        if (Objects.isNull(period)) {
            responseEntity = new ResponseEntity<>(emplService.findAllByEmployee(details.getUsername()), HttpStatus.OK);
        } else {
            try {
                responseEntity = new ResponseEntity<>(emplService.findAllByEmployeeWithPeriod(details.getUsername(), period).get(0), HttpStatus.OK);
            } catch (Exception ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
            }

        }
        return responseEntity;
    }
    @GetMapping("/api/admin/user")
    public ResponseEntity<List<User>> getUsers() {
        return new ResponseEntity<>(userService.getUsers(), HttpStatus.OK);
    }
    @GetMapping("/api/security/events")
    public ResponseEntity<List<SecurityEvent>> getSecurityEvents() {
        return new ResponseEntity<>(securityEventService.getSecurityEvents(), HttpStatus.OK);
    }
}


