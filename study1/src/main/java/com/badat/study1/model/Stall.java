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

    @Lob
    @Column(name = "stall_image_data", columnDefinition = "LONGBLOB")
    private byte[] stallImageData;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "PENDING";

    @Column(name = "is_active", nullable = false)
    private boolean isActive = false;

    @Column(name = "approval_reason", columnDefinition = "TEXT")
    private String approvalReason;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "is_delete", nullable = false)
    private boolean isDelete = false;

    @Transient
    private int productCount = 0;
    
    @Transient
    private String priceRange = "";

    // Constructors
    public Stall() {}

    public Stall(Long shopId, String stallName, String businessType, String stallCategory, 
                 Double discountPercentage, String shortDescription, String detailedDescription, 
                 byte[] stallImageData) {
        this.shopId = shopId;
        this.stallName = stallName;
        this.businessType = businessType;
        this.stallCategory = stallCategory;
        this.discountPercentage = discountPercentage;
        this.shortDescription = shortDescription;
        this.detailedDescription = detailedDescription;
        this.stallImageData = stallImageData;
        this.status = "PENDING";
        this.isActive = false;
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

    public byte[] getStallImageData() { return stallImageData; }
    public void setStallImageData(byte[] stallImageData) { this.stallImageData = stallImageData; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getApprovalReason() { return approvalReason; }
    public void setApprovalReason(String approvalReason) { this.approvalReason = approvalReason; }

    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }

    public Long getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Long approvedBy) { this.approvedBy = approvedBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public boolean isDelete() { return isDelete; }
    public void setDelete(boolean delete) { isDelete = delete; }

    public int getProductCount() { return productCount; }
    public void setProductCount(int productCount) { this.productCount = productCount; }

    public String getPriceRange() { return priceRange; }
    public void setPriceRange(String priceRange) { this.priceRange = priceRange; }
}
