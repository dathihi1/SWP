package com.badat.study1.service;

import com.badat.study1.model.ProductVariant;
import com.badat.study1.repository.ProductVariantRepository;
import org.springframework.stereotype.Service;

@Service
public class ProductService{

    private final ProductVariantRepository productVariantRepository;

    public ProductService(ProductVariantRepository productVariantRepository) {
        this.productVariantRepository = productVariantRepository;
    }

    public Long createProduct(ProductVariant productVariant){
        return productVariantRepository.save(productVariant).getId();
    }
}
