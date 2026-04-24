package com.example.payment_service.config;

import com.example.payment_service.filter.JwtAuthFilter;
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

    private final JwtAuthFilter jwtAuthFilter;
    
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String API_PAYMENTS = "/api/payments";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http){
        return http
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.GET,    API_PAYMENTS + "/summary/all").hasRole(ROLE_ADMIN)
                    .requestMatchers(HttpMethod.DELETE, API_PAYMENTS + "/**").hasRole(ROLE_ADMIN)
                    .requestMatchers(HttpMethod.PUT,    API_PAYMENTS + "/**").hasRole(ROLE_ADMIN)
                    .requestMatchers(API_PAYMENTS + "/**").authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}