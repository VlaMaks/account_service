package account.handler;

import account.entity.User;
import account.service.UserService;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
public class AuthenticationSuccessEventListener implements
        ApplicationListener<AuthenticationSuccessEvent> {

    private HttpServletRequest request;
    private UserService userService;

    public AuthenticationSuccessEventListener(HttpServletRequest request, UserService userService) {
        this.request = request;
        this.userService = userService;
    }

    @Override
    public void onApplicationEvent(final AuthenticationSuccessEvent e) {
        String login = e.getAuthentication().getName();
        User user = userService.findUserByEmail(login).get();
        if (user.getFailedAttempt() > 0) {
            userService.resetFailedAttempts(login);
        }
    }
}
