package com.badat.study1.configuration;

import com.badat.study1.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class AdminAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException, ServletException {
        
        User user = (User) authentication.getPrincipal();
        
        // Redirect admin users to admin dashboard
        if (user.getRole().name().equals("ADMIN")) {
            response.sendRedirect("/admin");
        } else {
            // Redirect other users to home page
            response.sendRedirect("/");
        }
    }
}
