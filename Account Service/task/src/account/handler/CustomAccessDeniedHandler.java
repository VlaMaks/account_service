package account.handler;

import account.entity.SecurityEventEnum;
import account.service.SecurityEventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import com.fasterxml.jackson.datatype.jsr310.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class CustomAccessDeniedHandler implements AccessDeniedHandler {
    private SecurityEventService securityEventService;

    public CustomAccessDeniedHandler(SecurityEventService securityEventService) {
        this.securityEventService = securityEventService;
    }

    private ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exc) throws IOException {
        securityEventService.saveEvent(SecurityEventEnum.ACCESS_DENIED, request.getRemoteUser(), request.getServletPath(), request.getServletPath());
        response.setContentType("application/json;charset=utf-8");
        response.setStatus(HttpStatus.FORBIDDEN.value());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("timestamp", LocalDateTime.now().toString());
        data.put("status", HttpStatus.FORBIDDEN.value());
        data.put("error", "Forbidden");
        data.put("message", "Access Denied!");
        data.put("path", request.getServletPath());

        response.getOutputStream()
                .println(objectMapper.writeValueAsString(data));
    }
}
