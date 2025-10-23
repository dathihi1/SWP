package com.badat.study1.repository;

import com.badat.study1.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    
    // Pagination methods
    Page<User> findByRole(User.Role role, Pageable pageable);
    Page<User> findByStatus(User.Status status, Pageable pageable);
    Page<User> findByRoleAndStatus(User.Role role, User.Status status, Pageable pageable);
    Page<User> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(
            String username, String email, String fullName, Pageable pageable);
    
    // Additional search methods for combined filters
    Page<User> findByUsernameContainingIgnoreCaseAndRole(String username, User.Role role, Pageable pageable);
    Page<User> findByUsernameContainingIgnoreCaseAndStatus(String username, User.Status status, Pageable pageable);
    Page<User> findByUsernameContainingIgnoreCaseAndRoleAndStatus(String username, User.Role role, User.Status status, Pageable pageable);
}
