package account.handler;

import account.entity.Role;
import account.entity.SecurityEventEnum;
import account.entity.Status;
import account.entity.User;
import account.service.SecurityEventService;
import account.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class AuthenticationFailureListener implements
        ApplicationListener<AuthenticationFailureBadCredentialsEvent> {

    private UserService userService;
    private SecurityEventService securityEventService;

    private HttpServletRequest request;
    private HttpServletResponse response;

    public AuthenticationFailureListener(UserService userService, SecurityEventService securityEventService, HttpServletRequest request, HttpServletResponse response) {
        this.userService = userService;
        this.securityEventService = securityEventService;
        this.request = request;
        this.response = response;
    }

    private ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();

    @Override

    public void onApplicationEvent(AuthenticationFailureBadCredentialsEvent e){
        securityEventService.saveEvent(SecurityEventEnum.LOGIN_FAILED, e.getAuthentication().getName().toLowerCase(), request.getServletPath(), request.getServletPath());

        String email = e.getAuthentication().getName();
        Optional<User> optUser = userService.findUserByEmail(email);

       // if (optUser.isPresent()) {
            User user = optUser.get();
            if (user.getStatus() == Status.ACTIVE) {
                userService.increaseFailedAttempts(user);

                if (user.getFailedAttempt() > UserService.MAX_FAILED_ATTEMPTS - 2) {
                    securityEventService.saveEvent(SecurityEventEnum.BRUTE_FORCE, e.getAuthentication().getName().toLowerCase(), request.getServletPath(), request.getServletPath());
                    securityEventService.saveEvent(SecurityEventEnum.LOCK_USER, e.getAuthentication().getName().toLowerCase(), securityEventService.getObject(SecurityEventEnum.LOCK_USER, "", e.getAuthentication().getName().toLowerCase()), request.getServletPath());
                    if (!user.getEmail().equalsIgnoreCase("johndoe@acme.com")) {
                        userService.lock(user);
                    }


                }
            }
        }

   // }
}
