package com.badat.study1.service;

import com.badat.study1.model.ProductVariant;
import com.badat.study1.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductVariantService {
    
    private final ProductVariantRepository productVariantRepository;
    
    public List<ProductVariant> getAvailableVariantsByProductId(Long productId) {
        return productVariantRepository.findAvailableVariantsByProductId(productId);
    }
    
    public List<ProductVariant> getVariantsByProductId(Long productId) {
        return productVariantRepository.findByProductIdAndIsDeleteFalse(productId);
    }
    
    public Optional<ProductVariant> getVariantById(Long id) {
        return productVariantRepository.findByIdAndIsDeleteFalse(id);
    }
    
    public Optional<ProductVariant> getVariantByUniqueKey(String uniqueKey) {
        return productVariantRepository.findByUniqueKeyAndIsDeleteFalse(uniqueKey);
    }
    
    public ProductVariant saveVariant(ProductVariant variant) {
        return productVariantRepository.save(variant);
    }
    
    public void deleteVariant(Long id) {
        Optional<ProductVariant> variant = productVariantRepository.findByIdAndIsDeleteFalse(id);
        if (variant.isPresent()) {
            ProductVariant v = variant.get();
            v.setIsDelete(true);
            productVariantRepository.save(v);
        }
    }
    
    public List<ProductVariant> getAvailableVariantsByProductIdAndStatus(Long productId, ProductVariant.Status status) {
        return productVariantRepository.findByProductIdAndStatusAndIsDeleteFalse(productId, status);
    }
}
