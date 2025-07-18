package org.example.dentalclinicmanagement.security.service;

import lombok.Getter;
import lombok.Setter;
import org.example.dentalclinicmanagement.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.*;

public class UserDetailsImpl implements UserDetails, OAuth2User {

    @Getter
    private final Long id;

    @Getter
    private final String email;
    private final String password;

    private final Collection<? extends GrantedAuthority> authorities;

    @Setter
    private Map<String, Object> attributes;

    public UserDetailsImpl(Long id, String email, String password,
                           Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
    }

    public static UserDetailsImpl build(User user) {
        Collection<SimpleGrantedAuthority> authorities = user.getRole().getAuthorities();

        return new UserDetailsImpl(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                authorities);
    }

    public static UserDetailsImpl build(User user, Map<String, Object> attributes) {
        UserDetailsImpl userDetails = build(user);
        userDetails.setAttributes(attributes);
        return userDetails;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes != null ? attributes : new HashMap<>();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getName() {
        return email;
    }
}
