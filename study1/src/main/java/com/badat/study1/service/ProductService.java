package com.badat.study1.service;

import com.badat.study1.model.Product;
import com.badat.study1.repository.ProductRepository;
import org.springframework.stereotype.Service;

@Service
public class ProductService{

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Long createProduct(Product product){
        return productRepository.save(product).getId();
    }
}
