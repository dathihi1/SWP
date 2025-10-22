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
@Table(name = "user")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity implements UserDetails {
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

    @Column(name = "username", unique = true, nullable = false, length = 50)
    String username;

    @Column(name = "password", nullable = false)
    String password;

    @Column(name = "email", unique = true, nullable = false, length = 100)
    String email;

    @Column(name = "phone", length = 20)
    String phone;

    @Column(name = "full_name", length = 100)
    String fullName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @Builder.Default
    Role role = Role.USER;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    Status status = Status.ACTIVE;
    
    @Column(name = "provider", length = 20)
    @Builder.Default
    String provider = "LOCAL";
    
    @Column(name = "provider_id", length = 100)
    String providerId;
    
    @Lob
    @Column(name = "avatar_data", columnDefinition = "LONGBLOB")
    byte[] avatarData;
    
    public enum Role {
        USER, ADMIN, SELLER
    }
    
    public enum Status {
         ACTIVE, LOCKED
    }
    
    // Explicit getter for status để đảm bảo Thymeleaf có thể access
    public Status getStatus() {
        return this.status;
    }
    
    // Explicit setter for status
    public void setStatus(Status status) {
        this.status = status;
    }
}
