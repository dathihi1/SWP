package com.badat.study1.controller;

import com.badat.study1.model.User;
import com.badat.study1.repository.UserRepository;
import com.badat.study1.repository.ShopRepository;
import com.badat.study1.repository.StallRepository;
import com.badat.study1.service.AuditLogService;
import com.badat.study1.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final AuditLogService auditLogService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final StallRepository stallRepository;

    // API thêm user
    @PostMapping("/users")
    public ResponseEntity<?> addUser(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
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
            String username = request.get("username");
            String email = request.get("email");
            String password = request.get("password");
            String role = request.get("role");
            if (username == null || username.trim().isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
            if (email == null || email.trim().isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
            if (password == null || password.trim().isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Password is required"));
            if (role == null || role.trim().isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Role is required"));
            if (userRepository.findByUsername(username).isPresent()) return ResponseEntity.badRequest().body(Map.of("error", "Username already exists"));
            if (userRepository.findByEmail(email).isPresent()) return ResponseEntity.badRequest().body(Map.of("error", "Email already exists"));
            User newUser = new User();
            newUser.setUsername(username.trim());
            newUser.setEmail(email.trim());
            newUser.setPassword(passwordEncoder.encode(password));
            newUser.setFullName(request.get("fullName") != null ? request.get("fullName").trim() : null);
            newUser.setPhone(request.get("phone") != null ? request.get("phone").trim() : null);
            try { newUser.setRole(User.Role.valueOf(role.toUpperCase())); } catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("error", "Invalid role: " + role)); }
            newUser.setStatus(User.Status.ACTIVE);
            newUser.setCreatedAt(java.time.LocalDateTime.now());
            userRepository.save(newUser);
            // Ghi log tạo mới
            if (auditLogService != null) {
                String clientIp = getClientIpAddress(httpRequest);
                auditLogService.logUserCreation(currentUser, newUser, httpRequest.getRequestURI(), httpRequest.getMethod(), clientIp);
            }
            log.info("Admin {} created new user {}", currentUser.getUsername(), newUser.getUsername());
            return ResponseEntity.ok(Map.of("message", "User created successfully", "userId", newUser.getId()));
        } catch (Exception e) {
            log.error("Error creating user: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }

    // API danh sách user
    @GetMapping("/users")
    public ResponseEntity<?> listUsers() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }
            User currentUser = (User) authentication.getPrincipal();
            if (!currentUser.getRole().name().equals("ADMIN")) {
                return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
            }
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

    // API chi tiết user
    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUserById(@PathVariable Long userId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }
            User currentUser = (User) authentication.getPrincipal();
            if (!currentUser.getRole().equals(User.Role.ADMIN)) {
                return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
            }
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) return ResponseEntity.status(404).body(Map.of("error", "User not found"));
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

    // API edit user
    @PutMapping("/users/{userId}")
    public ResponseEntity<?> editUser(@PathVariable Long userId, @RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }
            User currentUser = (User) authentication.getPrincipal();
            if (!currentUser.getRole().equals(User.Role.ADMIN)) {
                return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
            }
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            if (user.getRole() == User.Role.ADMIN) {
                return ResponseEntity.badRequest().body(Map.of("error", "Không thể chỉnh sửa Admin"));
            }
            User.Role oldRole = user.getRole();
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
            userRepository.save(user);
            if (auditLogService != null) {
                String clientIp = getClientIpAddress(httpRequest);
                auditLogService.logUserEdit(currentUser, user, request, httpRequest.getRequestURI(), httpRequest.getMethod(), clientIp);
            }
            log.info("Admin {} edited user {} information", currentUser.getUsername(), user.getUsername());
            return ResponseEntity.ok(Map.of("message", "User updated successfully", "userId", userId));
        } catch (Exception e) {
            log.error("Error editing user: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }

    // API lock user
    @PostMapping("/users/{userId}/lock")
    public ResponseEntity<?> lockUser(@PathVariable Long userId, HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }
            User currentUser = (User) authentication.getPrincipal();
            if (!currentUser.getRole().equals(User.Role.ADMIN)) {
                return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
            }
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            if (currentUser.getId().equals(user.getId()) && currentUser.getRole() == User.Role.ADMIN) {
                return ResponseEntity.badRequest().body(Map.of("error", "Admin không thể tự khóa chính mình"));
            }
            if (user.getStatus().equals(User.Status.LOCKED)) {
                return ResponseEntity.badRequest().body(Map.of("error", "User is already locked"));
            }
            user.setStatus(User.Status.LOCKED);
            userRepository.save(user);
            if (auditLogService != null) {
                String clientIp = getClientIpAddress(request);
                auditLogService.logAccountLocked(user, clientIp, "Locked by admin: " + currentUser.getUsername(), request.getRequestURI(), request.getMethod());
            }
            log.info("Admin {} locked user {}", currentUser.getUsername(), user.getUsername());
            return ResponseEntity.ok(Map.of("message", "User locked successfully", "userId", userId));
        } catch (Exception e) {
            log.error("Error locking user: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }

    // API unlock user
    @PostMapping("/users/{userId}/unlock")
    public ResponseEntity<?> unlockUser(@PathVariable Long userId, HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }
            User currentUser = (User) authentication.getPrincipal();
            if (!currentUser.getRole().equals(User.Role.ADMIN)) {
                return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
            }
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            if (user.getStatus().equals(User.Status.ACTIVE)) {
                return ResponseEntity.badRequest().body(Map.of("error", "User is already active"));
            }
            user.setStatus(User.Status.ACTIVE);
            userRepository.save(user);
            if (auditLogService != null) {
                String clientIp = getClientIpAddress(request);
                auditLogService.logAccountUnlocked(user, clientIp, request.getRequestURI(), request.getMethod());
            }
            log.info("Admin {} unlocked user {}", currentUser.getUsername(), user.getUsername());
            return ResponseEntity.ok(Map.of("message", "User unlocked successfully", "userId", userId));
        } catch (Exception e) {
            log.error("Error unlocking user: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }

    // API toggle-lock user
    @PostMapping("/users/{userId}/toggle-lock")
    public ResponseEntity<?> toggleUserLock(@PathVariable Long userId, HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }
            User currentUser = (User) authentication.getPrincipal();
            if (!currentUser.getRole().name().equals("ADMIN")) {
                return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
            }
            User userToToggle = userService.findById(userId);
            if (userToToggle == null) return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            if (currentUser.getId().equals(userToToggle.getId()) && currentUser.getRole() == User.Role.ADMIN && userToToggle.getStatus() == User.Status.ACTIVE) {
                return ResponseEntity.badRequest().body(Map.of("error", "Admin không thể tự khóa chính mình"));
            }
            if (userToToggle.getStatus() == User.Status.ACTIVE) {
                userToToggle.setStatus(User.Status.LOCKED);
                if (auditLogService != null) {
                    String clientIp = getClientIpAddress(request);
                    auditLogService.logAccountLocked(userToToggle, clientIp, "Locked by admin: " + currentUser.getUsername(), request.getRequestURI(), request.getMethod());
                }
            } else {
                userToToggle.setStatus(User.Status.ACTIVE);
                if (auditLogService != null) {
                    String clientIp = getClientIpAddress(request);
                    auditLogService.logAccountUnlocked(userToToggle, clientIp, request.getRequestURI(), request.getMethod());
                }
            }
            userService.save(userToToggle);
            return ResponseEntity.ok(Map.of("message", "User status updated successfully", "newStatus", userToToggle.getStatus().name()));
        } catch (Exception e) {
            log.error("Error toggling user lock: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }

    // Toggle-lock seller shop
    @PostMapping("/sellers/{sellerId}/toggle-lock")
    public ResponseEntity<?> toggleSellerLock(@PathVariable Long sellerId) {
        try {
            var shop = shopRepository.findByUserId(sellerId);
            if (shop.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Không tìm thấy cửa hàng"));
            }
            var shopEntity = shop.get();
            var currentStatus = shopEntity.getStatus();
            var newStatus = currentStatus == com.badat.study1.model.Shop.Status.ACTIVE ? com.badat.study1.model.Shop.Status.INACTIVE : com.badat.study1.model.Shop.Status.ACTIVE;
            shopEntity.setStatus(newStatus);
            shopEntity.setUpdatedAt(Instant.now());
            shopRepository.save(shopEntity);
            java.util.List<com.badat.study1.model.Stall> stalls = stallRepository.findByShopId(shopEntity.getId());
            boolean newActiveStatus = newStatus == com.badat.study1.model.Shop.Status.ACTIVE;
            for (com.badat.study1.model.Stall stall : stalls) {
                stall.setActive(newActiveStatus);
                if (newStatus == com.badat.study1.model.Shop.Status.INACTIVE) {
                    stall.setStatus("CLOSED");
                } else {
                    if ("CLOSED".equals(stall.getStatus())) {
                        stall.setStatus("OPEN");
                    }
                }
                stallRepository.save(stall);
            }
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Cập nhật trạng thái cửa hàng và gian hàng thành công",
                "shopStatus", newStatus.name(),
                "stallsUpdated", stalls.size()
            ));
        } catch (Exception e) {
            log.error("Error toggling seller lock for sellerId {}: {}", sellerId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Có lỗi xảy ra khi thực hiện thao tác"));
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        String xForwarded = request.getHeader("X-Forwarded");
        if (xForwarded != null && !xForwarded.isEmpty() && !"unknown".equalsIgnoreCase(xForwarded)) {
            return xForwarded;
        }
        String forwardedFor = request.getHeader("Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(forwardedFor)) {
            return forwardedFor;
        }
        String forwarded = request.getHeader("Forwarded");
        if (forwarded != null && !forwarded.isEmpty() && !"unknown".equalsIgnoreCase(forwarded)) {
            return forwarded;
        }
        return request.getRemoteAddr();
    }
}
