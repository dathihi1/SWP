package com.badat.study1.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "product")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Column(name = "product_category", length = 50)
    private String productCategory;

    @Column(name = "product_subcategory", length = 100)
    private String productSubcategory;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(name = "detailed_description", columnDefinition = "TEXT")
    private String detailedDescription;

    @Lob
    @Column(name = "product_image_data", columnDefinition = "LONGBLOB")
    private byte[] productImageData;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "OPEN";
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "is_delete", nullable = false)
    private boolean isDelete = false;

    @Transient
    private int productVariantCount = 0;
    
    @Transient
    private String priceRange = "";

    // Constructors
    public Product() {}

    public Product(Long shopId, String productName, String productCategory, 
                 String shortDescription, String detailedDescription, 
                 byte[] productImageData) {
        this.shopId = shopId;
        this.productName = productName;
        this.productCategory = productCategory;
        this.shortDescription = shortDescription;
        this.detailedDescription = detailedDescription;
        this.productImageData = productImageData;
        this.status = "OPEN";
        this.createdAt = Instant.now();
        this.isDelete = false;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductCategory() { return productCategory; }
    public void setProductCategory(String productCategory) { this.productCategory = productCategory; }

    public String getProductSubcategory() { return productSubcategory; }
    public void setProductSubcategory(String productSubcategory) { this.productSubcategory = productSubcategory; }

    public String getShortDescription() { return shortDescription; }
    public void setShortDescription(String shortDescription) { this.shortDescription = shortDescription; }

    public String getDetailedDescription() { return detailedDescription; }
    public void setDetailedDescription(String detailedDescription) { this.detailedDescription = detailedDescription; }

    public byte[] getProductImageData() { return productImageData; }
    public void setProductImageData(byte[] productImageData) { this.productImageData = productImageData; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // Approval fields removed; status uses only OPEN/CLOSED

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public boolean isDelete() { return isDelete; }
    public void setDelete(boolean delete) { isDelete = delete; }

    public int getProductVariantCount() { return productVariantCount; }
    public void setProductVariantCount(int productVariantCount) { this.productVariantCount = productVariantCount; }

    public String getPriceRange() { return priceRange; }
    public void setPriceRange(String priceRange) { this.priceRange = priceRange; }
}
