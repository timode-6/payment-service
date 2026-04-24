package com.example.payment_service.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class UserPrincipal implements UserDetails {

    private final String userId;
    private final String role;   

    public UserPrincipal(String userId, String role) {
        this.userId = userId;
        this.role   = role;
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
    }

    @Override 
    public String  getPassword(){
        return null; 
    }

    @Override
    public String  getUsername(){
        return userId; 
    }

    @Override
    public boolean isAccountNonExpired(){
        return true;
    }
    
    @Override 
    public boolean isAccountNonLocked(){ 
        return true; 
    }
    @Override
    public boolean isCredentialsNonExpired(){
        return true; 
    }

    @Override
    public boolean isEnabled(){
        return true; 
    }
}