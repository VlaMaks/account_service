package account.security;

import account.entity.Permission;
import account.handler.CustomAccessDeniedHandler;
import account.service.SecurityEventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final UserDetailsService userDetailsService;
    private final SecurityEventService securityEventService;

    public SecurityConfig(@Qualifier("userDetailsServiceImpl") UserDetailsService userDetailsService, SecurityEventService securityEventService) {
        this.userDetailsService = userDetailsService;
        this.securityEventService = securityEventService;
    }
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable().headers().frameOptions().disable()
                .and()
                .authorizeRequests()
                .antMatchers(HttpMethod.POST, "/api/auth/signup", "/actuator/shutdown").permitAll()
                .antMatchers(HttpMethod.GET, "/login", "/customError", "/access-denied", "/h2-console").permitAll()
                .antMatchers(HttpMethod.POST, "/api/auth/changepass").hasAuthority(Permission.CHANGE_PASS.getPermission())
                .antMatchers(HttpMethod.GET, "/api/empl/payment").hasAuthority(Permission.READ_PAYMENT.getPermission())
                .antMatchers(HttpMethod.POST, "/api/acct/payments").hasAuthority(Permission.WRITE_PAYMENT.getPermission())
                .antMatchers(HttpMethod.PUT, "/api/acct/payments").hasAuthority(Permission.WRITE_PAYMENT.getPermission())
                .antMatchers(HttpMethod.PUT, "/api/admin/user/role").hasAuthority(Permission.MANAGE_USER.getPermission())
                .antMatchers(HttpMethod.GET, "/api/admin/user").hasAuthority(Permission.MANAGE_USER.getPermission())
                .antMatchers("/api/admin/user/**").hasAuthority(Permission.MANAGE_USER.getPermission())
                .antMatchers(HttpMethod.GET, "/api/security/events/*").hasAuthority(Permission.READ_SECURITY_EVENTS.getPermission())
                .anyRequest().authenticated()
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .exceptionHandling()
                .accessDeniedHandler(customAccessDeniedHandler())
                .and()
                .httpBasic().authenticationEntryPoint(customEntryPoint());

        return http.build();
    }

    @Bean
    public AccessDeniedHandler customAccessDeniedHandler() {
        return new CustomAccessDeniedHandler(securityEventService);
    }

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();
    @Bean
    public AuthenticationEntryPoint customEntryPoint() {
        return (request, response, authException) -> {
            response.setContentType("application/json;charset=utf-8");
            response.setStatus(HttpStatus.UNAUTHORIZED.value());

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("timestamp", LocalDateTime.now().toString());
            data.put("status", HttpStatus.UNAUTHORIZED.value());
            data.put("error", "Unauthorized");
            data.put("message", authException.getMessage());
            data.put("path", request.getServletPath());

            try {
                response.getOutputStream()
                        .println(objectMapper.writeValueAsString(data));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        };
    }

    @Bean
    public static PasswordEncoder getEncoder() {
        return new BCryptPasswordEncoder(13);
    }

    @Bean
    protected DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
        daoAuthenticationProvider.setPasswordEncoder(getEncoder());
        daoAuthenticationProvider.setUserDetailsService(userDetailsService);
        return daoAuthenticationProvider;
    }

}



