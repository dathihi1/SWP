package com.badat.study1.repository;

import com.badat.study1.model.Shop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShopRepository extends JpaRepository<Shop, Long> {
    Optional<Shop> findByUserId(Long userId);
    Optional<Shop> findByCccd(String cccd);
    
    // Pagination methods
    Page<Shop> findByStatus(Shop.Status status, Pageable pageable);
    Page<Shop> findByIsDelete(Boolean isDelete, Pageable pageable);
    Page<Shop> findByStatusAndIsDelete(Shop.Status status, Boolean isDelete, Pageable pageable);
    Page<Shop> findByShopNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
        String shopName, String description, Pageable pageable);
    
    // List methods for dropdowns
    List<Shop> findByStatus(Shop.Status status);
    List<Shop> findByIsDelete(Boolean isDelete);
}


