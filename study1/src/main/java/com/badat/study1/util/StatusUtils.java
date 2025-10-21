package com.badat.study1.util;

import com.badat.study1.model.Order;

public class StatusUtils {
    
    public static String getStatusDisplayName(Order.Status status) {
        return switch (status) {
            case PENDING -> "Tạm giữ tiền";
            case COMPLETED -> "Hoàn thành";
            case CANCELLED -> "Đã hủy";
            case REFUNDED -> "Đã hoàn tiền";
        };
    }
}
