package account.controller;

import account.entity.EmpSal;
import account.entity.Role;
import account.entity.Status;
import account.entity.User;
import account.exception.PasswordExceptionReason;
import account.service.EmplService;
import account.service.UserService;
import account.validation.UserPasswordValidator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import account.security.SecurityConfig;

import javax.validation.Valid;
import javax.validation.constraints.Null;
import java.util.*;

@RestController
public class MainController {

    private final UserService userService;
    private final EmplService emplService;

    public MainController(UserService userService, EmplService emplService) {
        this.userService = userService;
        this.emplService = emplService;
    }

    @PostMapping("/api/auth/signup")
    public ResponseEntity<Object> signupUser(@Valid @RequestBody User user) {
        System.out.println(user);

        if (userService.signupUser(user)) {
            return new ResponseEntity(user, HttpStatus.OK);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User exist!");
        }

    }

    @PostMapping("api/auth/changepass")
    public ResponseEntity<Map<String, String>> changePassword(@RequestBody Map<String, String> bd, @AuthenticationPrincipal UserDetails details) {
        if (bd.size() != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "incorrect number of parameters");
        }

        ResponseEntity responseEntity = null;
        try {
            if (userService.changePassword(bd, details)) {
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

    @GetMapping("/api/auth/changepass")
    public ResponseEntity<User> checkAuth(@AuthenticationPrincipal UserDetails details) {
        System.out.print("checkAuth");
        System.out.println(details.getUsername());
        return new ResponseEntity<>(userService.checkAuth(details), HttpStatus.OK);
    }

    @GetMapping("api/empl/payment")
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


    /*@ExceptionHandler
    public ResponseEntity<Map<String, String>> handleAllException(Exception e){
        Map<String, String> response = new HashMap<>();
        String message = "";

        switch (e.getClass().getSimpleName()) {
            case "SeatNotFoundException":
            case "TokenNotFoundException":
            case "PurchaseException":
            case "WrongPasswordException":
                message = e.getMessage();
                break;
        }

        response.put("error", message);

        if ("The password is wrong!".equals(message)) {
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }*/

}


