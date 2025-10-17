package com.badat.study1.repository;

import com.badat.study1.model.Shop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShopRepository extends JpaRepository<Shop, Long> {
    Optional<Shop> findByUserId(Long userId);
    Optional<Shop> findByCccd(String cccd);
}


