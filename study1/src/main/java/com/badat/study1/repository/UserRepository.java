package com.badat.study1.repository;

import com.badat.study1.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmailAndIsDeleteFalse(String email);
    Optional<User> findByUsernameAndIsDeleteFalse(String username);
    
    List<User> findByRole(User.Role role);
    List<User> findByStatus(User.Status status);
    List<User> findByIsDeleteFalse();
}
