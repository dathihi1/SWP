package com.badat.study1.controller;

import com.badat.study1.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/avatar")
@RequiredArgsConstructor
public class AvatarApiController {
    
    private final UserService userService;
    
    @GetMapping("/{userId}")
    public ResponseEntity<byte[]> getAvatar(@PathVariable Long userId) {
        try {
            byte[] avatarData = userService.getAvatar(userId);
            
            if (avatarData != null && avatarData.length > 0) {
                HttpHeaders headers = new HttpHeaders();
                String contentType = detectContentType(avatarData);
                headers.setContentType(MediaType.valueOf(contentType));
                headers.setContentLength(avatarData.length);
                headers.setCacheControl("public, max-age=3600");
                
                return ResponseEntity.ok()
                        .headers(headers)
                        .body(avatarData);
            } else {
                return getDefaultAvatarResponse();
            }
            
        } catch (Exception e) {
            log.error("Error getting avatar for user {}: {}", userId, e.getMessage());
            return getDefaultAvatarResponse();
        }
    }

    @GetMapping("/my-avatar")
    public ResponseEntity<byte[]> getMyAvatar() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() ||
                authentication.getName().equals("anonymousUser")) {
                return getDefaultAvatarResponse();
            }
            
            // Get user from authentication
            Object principal = authentication.getPrincipal();
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                // This is a UserDetails object, we need to get the actual User
                // You might need to fetch the user by username here
                return getDefaultAvatarResponse();
            } else {
                // This is our custom User object
                com.badat.study1.model.User currentUser = (com.badat.study1.model.User) principal;
                byte[] avatarData = userService.getAvatar(currentUser.getId());
                
                if (avatarData != null && avatarData.length > 0) {
                    HttpHeaders headers = new HttpHeaders();
                    String contentType = detectContentType(avatarData);
                    headers.setContentType(MediaType.valueOf(contentType));
                    headers.setContentLength(avatarData.length);
                    headers.setCacheControl("public, max-age=3600");
                    return ResponseEntity.ok().headers(headers).body(avatarData);
                }
            }
            
            return getDefaultAvatarResponse();
        } catch (Exception e) {
            log.error("Error getting current user's avatar: {}", e.getMessage());
            return getDefaultAvatarResponse();
        }
    }
    
    private ResponseEntity<byte[]> getDefaultAvatarResponse() {
        try {
            byte[] defaultAvatar = userService.getDefaultAvatar();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf("image/svg+xml"));
            headers.setContentLength(defaultAvatar.length);
            headers.setCacheControl("public, max-age=3600");
            return ResponseEntity.ok().headers(headers).body(defaultAvatar);
        } catch (Exception e) {
            log.error("Error getting default avatar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    private String detectContentType(byte[] data) {
        if (data.length < 4) {
            return "image/svg+xml";
        }
        
        // JPEG: FF D8 FF
        if (data[0] == (byte) 0xFF && data[1] == (byte) 0xD8 && data[2] == (byte) 0xFF) {
            return "image/jpeg";
        }
        
        // PNG: 89 50 4E 47
        if (data[0] == (byte) 0x89 && data[1] == (byte) 0x50 && data[2] == (byte) 0x4E && data[3] == (byte) 0x47) {
            return "image/png";
        }
        
        // GIF: 47 49 46 38
        if (data[0] == (byte) 0x47 && data[1] == (byte) 0x49 && data[2] == (byte) 0x46 && data[3] == (byte) 0x38) {
            return "image/gif";
        }
        
        // SVG: Check for SVG content
        String content = new String(data, 0, Math.min(100, data.length));
        if (content.contains("<svg") || content.contains("<?xml")) {
            return "image/svg+xml";
        }
        
        return "image/png";
    }
}
