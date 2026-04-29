package com.example.payment_service.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

@Getter
public class UserPrincipal {

    private final String userId;
    private final String role;

    public UserPrincipal(String userId, String role) {
        this.userId = userId;
        this.role   = role;
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    public Collection<GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
    }

    public static UserPrincipal of(String userId, String role) {
        return new UserPrincipal(userId, role);
    }
}