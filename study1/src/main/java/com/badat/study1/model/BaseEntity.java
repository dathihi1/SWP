package com.badat.study1.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseEntity {
    
    @Column(name = "isDelete", nullable = false)
    private Boolean isDelete = false;
    
    @CreatedBy
    @Column(name = "createdBy", length = 50)
    private String createdBy;
    
    @CreatedDate
    @Column(name = "createdAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updatedAt")
    private LocalDateTime updatedAt;
    
    @Column(name = "deletedBy", length = 50)
    private String deletedBy;
}
