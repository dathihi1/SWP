package com.badat.study1.repository;

import com.badat.study1.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByShopId(Long shopId);
    List<Product> findByShopIdAndIsDeleteFalse(Long shopId);
    List<Product> findByIsDeleteFalse();
    List<Product> findByStatus(Product.Status status);
    List<Product> findByType(String type);
    List<Product> findByNameContainingIgnoreCase(String name);
    Optional<Product> findByUniqueKey(String uniqueKey);
}