package com.example.payment_service.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class InternalAuthFilter extends OncePerRequestFilter {

    public static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";
    public static final String USER_ID_HEADER         = "X-User-Id";
    public static final String USER_ROLE_HEADER       = "X-User-Role";

    private static final List<String> PUBLIC_PATHS = List.of(
            "/actuator/health",
            "/actuator/info"
    );

    @Value("${gateway.internal-secret}")
    private String expectedSecret;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(
            @NotNull HttpServletRequest  request,
            @NotNull HttpServletResponse response,
            @NotNull FilterChain         chain
    ) throws ServletException, IOException {

        String incomingSecret = request.getHeader(INTERNAL_SECRET_HEADER);

        if (!expectedSecret.equals(incomingSecret)) {
            log.warn("Blocked direct access attempt to {} — missing or wrong {}",
                    request.getRequestURI(), INTERNAL_SECRET_HEADER);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Direct service access not allowed\"}");
            return;
        }

        String userId = request.getHeader(USER_ID_HEADER);
        String role   = request.getHeader(USER_ROLE_HEADER);

        UsernamePasswordAuthenticationToken auth;

        if (userId != null && !userId.isBlank() && role != null && !role.isBlank()) {
            auth = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
            );
            log.debug("Authenticated request: userId={} role={} path={}",
                    userId, role, request.getRequestURI());
        } else {
            auth = new UsernamePasswordAuthenticationToken(
                    "internal-system",
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_INTERNAL"))
            );
            log.debug("Internal system request to {}", request.getRequestURI());
        }

        SecurityContextHolder.getContext().setAuthentication(auth);
        chain.doFilter(request, response);
    }
}