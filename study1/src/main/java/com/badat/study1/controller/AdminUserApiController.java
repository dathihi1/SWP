package com.badat.study1.controller;

import com.badat.study1.model.User;
import com.badat.study1.model.Shop;
import com.badat.study1.repository.UserRepository;
import com.badat.study1.repository.ShopRepository;
import com.badat.study1.service.AuditLogService;
import com.badat.study1.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserApiController {
    
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final AuditLogService auditLogService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    
    @PostMapping("/add")
    public ResponseEntity<?> addUser(@RequestBody Map<String, String> request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                "anonymousUser".equals(authentication.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            User currentUser = (User) authentication.getPrincipal();
            if (!currentUser.getRole().equals(User.Role.ADMIN)) {
                return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
            }

            // Validate required fields
            String username = request.get("username");
            String email = request.get("email");
            String password = request.get("password");
            String role = request.get("role");

            if (username == null || username.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
            }
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
            }
            if (password == null || password.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Password is required"));
            }
            if (role == null || role.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Role is required"));
            }

            // Check if username already exists
            if (userRepository.findByUsername(username).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username already exists"));
            }

            // Check if email already exists
            if (userRepository.findByEmail(email).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email already exists"));
            }

            // Create new user
            User newUser = new User();
            newUser.setUsername(username.trim());
            newUser.setEmail(email.trim());
            newUser.setPassword(passwordEncoder.encode(password)); // BCrypt encode password
            newUser.setFullName(request.get("fullName") != null ? request.get("fullName").trim() : null);
            newUser.setPhone(request.get("phone") != null ? request.get("phone").trim() : null);
            // Set role from request
            try {
                newUser.setRole(User.Role.valueOf(role.toUpperCase()));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid role: " + role));
            }
            newUser.setStatus(User.Status.ACTIVE);
            newUser.setCreatedAt(java.time.LocalDateTime.now());

            userRepository.save(newUser);

            // Log the user creation
            if (auditLogService != null) {
                auditLogService.logUserCreation(currentUser, newUser);
            }

            log.info("Admin {} created new user {}", currentUser.getUsername(), newUser.getUsername());

            return ResponseEntity.ok(Map.of(
                "message", "User created successfully",
                "userId", newUser.getId()
            ));

        } catch (Exception e) {
            log.error("Error creating user: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
    

    @GetMapping("/list")
    public ResponseEntity<?> listUsers() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                "anonymousUser".equals(authentication.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            User currentUser = (User) authentication.getPrincipal();
            if (!currentUser.getRole().name().equals("ADMIN")) {
                return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
            }

            // Get all users (including deleted ones for debugging)
            var allUsers = userRepository.findAll();
            var activeUsers = userRepository.findByIsDeleteFalse();
            
            return ResponseEntity.ok(Map.of(
                "totalUsers", allUsers.size(),
                "activeUsers", activeUsers.size(),
                "users", allUsers.stream().map(user -> Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "role", user.getRole().name(),
                    "status", user.getStatus().name(),
                    "provider", user.getProvider(),
                    "isDelete", user.getIsDelete(),
                    "createdAt", user.getCreatedAt(),
                    "createdBy", user.getCreatedBy()
                )).toList()
            ));

        } catch (Exception e) {
            log.error("Error listing users: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }

    @PostMapping("/{userId}/toggle-lock")
    public ResponseEntity<?> toggleUserLock(@PathVariable Long userId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                "anonymousUser".equals(authentication.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            User currentUser = (User) authentication.getPrincipal();
            if (!currentUser.getRole().name().equals("ADMIN")) {
                return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
            }

            // Get user to toggle
            User userToToggle = userService.findById(userId);
            if (userToToggle == null) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }

            // Toggle status
            if (userToToggle.getStatus() == User.Status.ACTIVE) {
                userToToggle.setStatus(User.Status.LOCKED);
                log.info("Admin {} locked user {}", currentUser.getUsername(), userToToggle.getUsername());
            } else {
                userToToggle.setStatus(User.Status.ACTIVE);
                log.info("Admin {} unlocked user {}", currentUser.getUsername(), userToToggle.getUsername());
            }

            userService.save(userToToggle);

            return ResponseEntity.ok(Map.of(
                "message", "User status updated successfully",
                "newStatus", userToToggle.getStatus().name()
            ));

        } catch (Exception e) {
            log.error("Error toggling user lock: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
    
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserById(@PathVariable Long userId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                "anonymousUser".equals(authentication.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            User currentUser = (User) authentication.getPrincipal();
            if (!currentUser.getRole().equals(User.Role.ADMIN)) {
                return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
            }

            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }

            return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "fullName", user.getFullName() != null ? user.getFullName() : "",
                "phone", user.getPhone() != null ? user.getPhone() : "",
                "role", user.getRole().name(),
                "status", user.getStatus().name(),
                "createdAt", user.getCreatedAt()
            ));

        } catch (Exception e) {
            log.error("Error getting user by ID: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }

    @PutMapping("/{userId}/edit")
    public ResponseEntity<?> editUser(@PathVariable Long userId, @RequestBody Map<String, String> request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                "anonymousUser".equals(authentication.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            User currentUser = (User) authentication.getPrincipal();
            if (!currentUser.getRole().equals(User.Role.ADMIN)) {
                return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
            }

            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }

            // Store old role for shop management
            User.Role oldRole = user.getRole();
            
            // Update user fields (username and email cannot be changed)
            if (request.containsKey("fullName")) {
                user.setFullName(request.get("fullName"));
            }
            if (request.containsKey("phone")) {
                user.setPhone(request.get("phone"));
            }
            if (request.containsKey("role")) {
                try {
                    User.Role newRole = User.Role.valueOf(request.get("role").toUpperCase());
                    user.setRole(newRole);
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Invalid role"));
                }
            }
            if (request.containsKey("status")) {
                try {
                    User.Status newStatus = User.Status.valueOf(request.get("status").toUpperCase());
                    user.setStatus(newStatus);
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Invalid status"));
                }
            }
            
            // Handle shop creation/deletion based on role change
            if (user.getRole() == User.Role.SELLER && oldRole != User.Role.SELLER) {
                // Create shop for new seller
                createShopForSeller(user);
            } else if (oldRole == User.Role.SELLER && user.getRole() != User.Role.SELLER) {
                // Delete shop when removing seller role
                deleteShopForUser(user.getId());
            }

            userRepository.save(user);

            // Log the edit action
            if (auditLogService != null) {
                auditLogService.logUserEdit(currentUser, user, request);
            }

            log.info("Admin {} edited user {} information", currentUser.getUsername(), user.getUsername());

            return ResponseEntity.ok(Map.of(
                "message", "User updated successfully",
                "userId", userId
            ));

        } catch (Exception e) {
            log.error("Error editing user: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }

    @PostMapping("/{userId}/lock")
    public ResponseEntity<?> lockUser(@PathVariable Long userId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                "anonymousUser".equals(authentication.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            User currentUser = (User) authentication.getPrincipal();
            if (!currentUser.getRole().equals(User.Role.ADMIN)) {
                return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
            }

            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }

            if (user.getStatus().equals(User.Status.LOCKED)) {
                return ResponseEntity.badRequest().body(Map.of("error", "User is already locked"));
            }

            user.setStatus(User.Status.LOCKED);
            userRepository.save(user);

            // Log the lock action
            if (auditLogService != null) {
                auditLogService.logAccountLocked(user, "127.0.0.1", "Locked by admin: " + currentUser.getUsername());
            }

            log.info("Admin {} locked user {}", currentUser.getUsername(), user.getUsername());

            return ResponseEntity.ok(Map.of(
                "message", "User locked successfully",
                "userId", userId
            ));

        } catch (Exception e) {
            log.error("Error locking user: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }

    @PostMapping("/{userId}/unlock")
    public ResponseEntity<?> unlockUser(@PathVariable Long userId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                "anonymousUser".equals(authentication.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            User currentUser = (User) authentication.getPrincipal();
            if (!currentUser.getRole().equals(User.Role.ADMIN)) {
                return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
            }

            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }

            if (user.getStatus().equals(User.Status.ACTIVE)) {
                return ResponseEntity.badRequest().body(Map.of("error", "User is already active"));
            }

            user.setStatus(User.Status.ACTIVE);
            userRepository.save(user);

            // Log the unlock action
            if (auditLogService != null) {
                auditLogService.logAccountUnlocked(user, "127.0.0.1");
            }

            log.info("Admin {} unlocked user {}", currentUser.getUsername(), user.getUsername());

            return ResponseEntity.ok(Map.of(
                "message", "User unlocked successfully",
                "userId", userId
            ));

        } catch (Exception e) {
            log.error("Error unlocking user: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
    
    @PostMapping("/{userId}/change-role")
    public ResponseEntity<?> changeUserRole(@PathVariable Long userId, @RequestBody Map<String, String> request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                "anonymousUser".equals(authentication.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            User currentUser = (User) authentication.getPrincipal();
            if (!currentUser.getRole().equals(User.Role.ADMIN)) {
                return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
            }

            // Get user to change role
            User targetUser = userRepository.findById(userId).orElse(null);
            if (targetUser == null) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }

            String newRoleStr = request.get("newRole");
            String reason = request.get("reason");

            if (newRoleStr == null || newRoleStr.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "New role is required"));
            }

            User.Role newRole;
            try {
                newRole = User.Role.valueOf(newRoleStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid role"));
            }

            // Check if role is already the same
            if (targetUser.getRole().equals(newRole)) {
                return ResponseEntity.badRequest().body(Map.of("error", "User already has this role"));
            }

            // Store old role for audit
            User.Role oldRole = targetUser.getRole();
            
            // Handle shop creation/deletion based on role change
            if (newRole == User.Role.SELLER && oldRole != User.Role.SELLER) {
                // Create shop for new seller
                createShopForSeller(targetUser);
            } else if (oldRole == User.Role.SELLER && newRole != User.Role.SELLER) {
                // Delete shop when removing seller role
                deleteShopForUser(targetUser.getId());
            }
            
            // Change role
            targetUser.setRole(newRole);
            userRepository.save(targetUser);

            // Log the role change
            if (auditLogService != null) {
                auditLogService.logRoleChange(currentUser, targetUser, oldRole, newRole, reason);
            }

            log.info("Admin {} changed user {} role from {} to {}", 
                currentUser.getUsername(), targetUser.getUsername(), oldRole, newRole);

            return ResponseEntity.ok(Map.of(
                "message", "Role changed successfully",
                "oldRole", oldRole.name(),
                "newRole", newRole.name(),
                "userId", userId
            ));

        } catch (Exception e) {
            log.error("Error changing user role: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
    
    private void createShopForSeller(User user) {
        try {
            // Check if shop already exists
            if (shopRepository.findByUserId(user.getId()).isPresent()) {
                log.warn("Shop already exists for user {}", user.getId());
                return;
            }
            
            // Create new shop record in database
            Shop shop = Shop.builder()
                .userId(user.getId())
                .shopName(user.getFullName() != null ? user.getFullName() + "'s Shop" : user.getUsername() + "'s Shop")
                .cccd("") // Will be filled later by seller
                .bankAccountId(1L) // Default bank account, seller can change later
                .status(Shop.Status.ACTIVE)
                .createdAt(java.time.Instant.now())
                .isDelete(false)
                .build();
                
            shopRepository.save(shop);
            log.info("Created new shop record for user {} with shop ID {}", user.getId(), shop.getId());
            
        } catch (Exception e) {
            log.error("Error creating shop for user {}: {}", user.getId(), e.getMessage(), e);
        }
    }
    
    private void deleteShopForUser(Long userId) {
        try {
            shopRepository.findByUserId(userId).ifPresent(shop -> {
                shop.setIsDelete(true);
                shop.setUpdatedAt(java.time.Instant.now());
                shopRepository.save(shop);
                log.info("Marked shop as deleted for user {}", userId);
            });
        } catch (Exception e) {
            log.error("Error deleting shop for user {}: {}", userId, e.getMessage(), e);
        }
    }
    
    @GetMapping("/pending-sellers")
    public ResponseEntity<?> getPendingSellers() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                "anonymousUser".equals(authentication.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            User currentUser = (User) authentication.getPrincipal();
            if (!currentUser.getRole().equals(User.Role.ADMIN)) {
                return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
            }

            // Get users with PENDING_SELLER role (if you have this role)
            // For now, we'll get users who registered as sellers but haven't been approved
            // You might need to add a PENDING_SELLER role or use a different approach
            
            return ResponseEntity.ok(Map.of(
                "message", "Pending sellers retrieved successfully",
                "count", 0 // Placeholder
            ));

        } catch (Exception e) {
            log.error("Error getting pending sellers: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
}
