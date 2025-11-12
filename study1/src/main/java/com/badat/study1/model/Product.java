package com.badat.study1.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
}
