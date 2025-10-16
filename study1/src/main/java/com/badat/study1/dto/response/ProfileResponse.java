package com.badat.study1.dto.response;

import com.badat.study1.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private User.Role role;
    private User.Status status;
    private LocalDateTime createdAt;
    
    // Additional profile stats
    private BigDecimal walletBalance;
    private Long totalOrders;
    private Long totalShops;
    private Long totalSales;
}
