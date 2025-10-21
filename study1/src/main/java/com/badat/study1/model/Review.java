package com.badat.study1.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "review")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    
    @Column(name = "order_id", nullable = false)
    Long orderId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    Order order;
    
    @Column(name = "product_id", nullable = false)
    Long productId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    Product product;
    
    @Column(name = "buyer_id", nullable = false)
    Long buyerId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", insertable = false, updatable = false)
    User buyer;
    
    @Column(name = "seller_id", nullable = false)
    Long sellerId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", insertable = false, updatable = false)
    User seller;
    
    @Column(name = "shop_id", nullable = false)
    Long shopId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", insertable = false, updatable = false)
    Shop shop;
    
    @Column(name = "stall_id", nullable = false)
    Long stallId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stall_id", insertable = false, updatable = false)
    Stall stall;
    
    @Column(name = "rating", nullable = false)
    @Min(1)
    @Max(5)
    Integer rating;
    
    @Column(name = "title", length = 255)
    String title;
    
    @Column(name = "content", columnDefinition = "TEXT")
    String content;
    
    @Column(name = "reply_content", columnDefinition = "TEXT")
    String replyContent;
    
    @Column(name = "reply_at")
    java.time.LocalDateTime replyAt;
    
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    Boolean isRead = false;
}
