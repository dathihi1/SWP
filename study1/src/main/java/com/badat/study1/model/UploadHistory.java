package com.badat.study1.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "upload_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "file_name", nullable = false)
    private String fileName;
    
    @Column(name = "product_name", nullable = false)
    private String productName;
    
    @Column(name = "is_success", nullable = false)
    private Boolean isSuccess;
    
    @Column(name = "result", length = 500)
    private String result;
    
    @Column(name = "status", length = 50)
    private String status;
    
    @Column(name = "result_details", columnDefinition = "TEXT")
    private String resultDetails;
    
    @Column(name = "total_items")
    private Integer totalItems;
    
    @Column(name = "success_count")
    private Integer successCount;
    
    @Column(name = "failure_count")
    private Integer failureCount;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "is_delete", nullable = false)
    @Builder.Default
    private Boolean isDelete = false;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @CreationTimestamp
    @Column(name = "upload_date", nullable = false, updatable = false)
    private Instant uploadDate;
}
