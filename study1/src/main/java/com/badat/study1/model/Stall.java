package com.badat.study1.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "stall")
public class Stall {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    @Column(name = "stall_name", nullable = false, length = 100)
    private String stallName;

    @Column(name = "business_type", length = 50)
    private String businessType;

    @Column(name = "stall_category", length = 50)
    private String stallCategory;

    @Column(name = "discount_percentage")
    private Double discountPercentage;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(name = "detailed_description", columnDefinition = "TEXT")
    private String detailedDescription;

    @Column(name = "stall_image_url", length = 500)
    private String stallImageUrl;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "OPEN";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "is_delete", nullable = false)
    private boolean isDelete = false;

    // Constructors
    public Stall() {}

    public Stall(Long shopId, String stallName, String businessType, String stallCategory, 
                 Double discountPercentage, String shortDescription, String detailedDescription, 
                 String stallImageUrl) {
        this.shopId = shopId;
        this.stallName = stallName;
        this.businessType = businessType;
        this.stallCategory = stallCategory;
        this.discountPercentage = discountPercentage;
        this.shortDescription = shortDescription;
        this.detailedDescription = detailedDescription;
        this.stallImageUrl = stallImageUrl;
        this.createdAt = Instant.now();
        this.isDelete = false;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }

    public String getStallName() { return stallName; }
    public void setStallName(String stallName) { this.stallName = stallName; }

    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }

    public String getStallCategory() { return stallCategory; }
    public void setStallCategory(String stallCategory) { this.stallCategory = stallCategory; }

    public Double getDiscountPercentage() { return discountPercentage; }
    public void setDiscountPercentage(Double discountPercentage) { this.discountPercentage = discountPercentage; }

    public String getShortDescription() { return shortDescription; }
    public void setShortDescription(String shortDescription) { this.shortDescription = shortDescription; }

    public String getDetailedDescription() { return detailedDescription; }
    public void setDetailedDescription(String detailedDescription) { this.detailedDescription = detailedDescription; }

    public String getStallImageUrl() { return stallImageUrl; }
    public void setStallImageUrl(String stallImageUrl) { this.stallImageUrl = stallImageUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public boolean isDelete() { return isDelete; }
    public void setDelete(boolean delete) { isDelete = delete; }
}
