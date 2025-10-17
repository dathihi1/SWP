package com.badat.study1.admin.dto;

import java.math.BigDecimal;

public class StatsDTO {
    private long totalUsers;
    private long totalShops;
    private long totalProducts;
    private long totalOrders;
    private long totalDisputes;
    private long totalWithdrawsPending;
    private BigDecimal gmV; // gross merchandise volume

    // getters/setters

    public long getTotalUsers() {
        return totalUsers;
    }
    // ... (generate)

    public long getTotalShops() {
        return totalShops;
    }

    public long getTotalProducts() {
        return totalProducts;
    }

    public long getTotalOrders() {
        return totalOrders;
    }

    public long getTotalWithdrawsPending() {
        return totalWithdrawsPending;
    }

    public BigDecimal getGmV() {
        return gmV;
    }

    public long getTotalDisputes() {
        return totalDisputes;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public void setTotalShops(long totalShops) {
        this.totalShops = totalShops;
    }

    public void setTotalProducts(long totalProducts) {
        this.totalProducts = totalProducts;
    }

    public void setTotalOrders(long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public void setTotalDisputes(long totalDisputes) {
        this.totalDisputes = totalDisputes;
    }

    public void setTotalWithdrawsPending(long totalWithdrawsPending) {
        this.totalWithdrawsPending = totalWithdrawsPending;
    }

    public void setGmV(BigDecimal gmV) {
        this.gmV = gmV;
    }
}
