package account.service;

import account.entity.SecurityEvent;
import account.entity.SecurityEventEnum;
import account.repository.SecurityEventRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SecurityEventService {

    private SecurityEventRepository securityEventRepository;

    public SecurityEventService(SecurityEventRepository securityEventRepository) {
        this.securityEventRepository = securityEventRepository;
    }

    public void saveEvent(SecurityEventEnum action, String subject, String object, String path) {
        SecurityEvent securityEvent = new SecurityEvent();
        securityEvent.setDate(LocalDateTime.now());
        securityEvent.setAction(action);
        if ("".equals(subject)) {
            securityEvent.setSubject("Anonymous");
        } else {
            securityEvent.setSubject(subject);
        }
        securityEvent.setObject(object);
        securityEvent.setPath(path);
        securityEventRepository.save(securityEvent);
    }

    public String getObject(SecurityEventEnum action, String role, String user) {
        if (action == SecurityEventEnum.GRANT_ROLE)
            return String.format("Grant role %s to %s", role, user.toLowerCase());
        else if (action == SecurityEventEnum.REMOVE_ROLE) {
            return String.format("Remove role %s from %s", role, user.toLowerCase());
        } else if (action == SecurityEventEnum.LOCK_USER) {
            return String.format("Lock user %s", user.toLowerCase());
        } else if (action == SecurityEventEnum.UNLOCK_USER) {
            return String.format("Unlock user %s", user.toLowerCase());
        }
        else {
            return user;
        }
    }

    public List<SecurityEvent> getSecurityEvents() {
        return securityEventRepository.findAll(Sort.by("id"));
    }
}
