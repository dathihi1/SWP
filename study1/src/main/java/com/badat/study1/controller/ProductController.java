package com.badat.study1.controller;

import com.badat.study1.model.Product;
import com.badat.study1.model.ProductVariant;
import com.badat.study1.repository.ProductRepository;
import com.badat.study1.repository.ProductVariantRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
public class ProductController {
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    public ProductController(ProductRepository productRepository, ProductVariantRepository productVariantRepository) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
    }

    @PostMapping("/api/products")
    public Product createProduct(@RequestBody Product product) {
        return productRepository.save(product);
    }

    @GetMapping("/api/products/{productId}/variants")
    public List<ProductVariant> getProductVariants(@PathVariable Long productId) {
        return productVariantRepository.findAvailableVariantsByProductId(productId);
    }

    @GetMapping("/api/variants/{variantId}")
    public Optional<ProductVariant> getVariantById(@PathVariable Long variantId) {
        return productVariantRepository.findByIdAndIsDeleteFalse(variantId);
    }
}
