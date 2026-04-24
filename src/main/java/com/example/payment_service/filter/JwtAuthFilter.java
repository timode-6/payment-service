package com.example.payment_service.filter;


import com.example.payment_service.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {


    @Value("${gateway.internal-secret}")
    private String expectedSecret;
    
    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest  request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain         chain
    ) throws ServletException, IOException {
        
        String incomingSecret = request.getHeader("X-Internal-Secret");

        if (!expectedSecret.equals(incomingSecret)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Direct service access not allowed\"}");
            return;
        }

        String authHeader = request.getHeader("Authorization");

        jwtService.extractPrincipal(authHeader).ifPresent(principal -> {
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                principal, null, principal.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        });

        chain.doFilter(request, response);
    }
}