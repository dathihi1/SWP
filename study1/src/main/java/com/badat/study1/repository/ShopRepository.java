package com.badat.study1.repository;

import com.badat.study1.model.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Long> {
    Optional<Shop> findByUserId(Long userId);
    Optional<Shop> findByUserIdAndIsDeleteFalse(Long userId);
    List<Shop> findByIsDeleteFalse();
    List<Shop> findByShopNameContainingIgnoreCase(String shopName);
}
