package com.badat.study1.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "User")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return username; // trả về username để hiển thị
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // cho phép đăng nhập
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // cho phép đăng nhập
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // cho phép đăng nhập
    }

    @Override
    public boolean isEnabled() {
        return true; // cho phép đăng nhập
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(unique = true, nullable = false)
    String email;

    @Column(unique = true, nullable = false)
    String username;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    Role role = Role.USER;
    
    String password;
    
    public enum Role {
        USER, ADMIN, SELLER
    }
}
