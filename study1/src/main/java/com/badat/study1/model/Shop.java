package com.badat.study1.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "shop")
public class Shop {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "shop_name", nullable = false, length = 100)
    private String shopName;

    @Column(name = "cccd")
    private String cccd;

    @Column(name = "bank_account_id", nullable = false)
    private Long bankAccountId;

    // Additional columns present in DB schema
    @Column(name = "created_at", nullable = false)
    private Instant createdAtLower;

    @Column(name = "is_delete", nullable = false)
    private boolean isDelete;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }

    public String getCccd() { return cccd; }
    public void setCccd(String cccd) { this.cccd = cccd; }

    public Long getBankAccountId() { return bankAccountId; }
    public void setBankAccountId(Long bankAccountId) { this.bankAccountId = bankAccountId; }

    public Instant getCreatedAtLower() { return createdAtLower; }
    public void setCreatedAtLower(Instant createdAtLower) { this.createdAtLower = createdAtLower; }

    public boolean isDelete() { return isDelete; }
    public void setDelete(boolean delete) { isDelete = delete; }
}


