package com.badat.study1.controller;

import com.badat.study1.model.Product;
import com.badat.study1.repository.ProductRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductController {
    private final ProductRepository productRepository;
    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @PostMapping("/templates/products")
    public Product createProduct(@RequestBody Product product){
        return productRepository.save(product);
    }
}
