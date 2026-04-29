package com.example.payment_service.config;

import com.example.payment_service.filter.InternalAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final InternalAuthFilter internalAuthFilter;

    private static final String ADMIN = "ADMIN";

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http){
        return http
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(internalAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/payments/**").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PUT,    "/api/payments/**").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/payments/summary/all")
                            .hasRole(ADMIN)
                        .anyRequest().authenticated()
                )
                .build();
    }
}