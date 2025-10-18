package com.badat.study1.controller;

import com.badat.study1.model.Product;
import com.badat.study1.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ProductBrowseController {

    private final ProductRepository productRepository;

    @GetMapping("/products")
    public String listProducts(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "type", required = false) String type,
            Model model) {
        List<Product> products;
        if (query != null && !query.isBlank()) {
            products = productRepository.findByNameContainingIgnoreCaseAndIsDeleteFalseAndStatus(
                    query.trim(), Product.Status.AVAILABLE);
        } else {
            products = productRepository.findByIsDeleteFalseAndStatus(Product.Status.AVAILABLE);
        }
        // Optional in-memory filter by type to avoid adding more repo methods
        if (type != null && !type.isBlank()) {
            String filterType = type.trim();
            products = products.stream()
                    .filter(p -> filterType.equalsIgnoreCase(p.getType()))
                    .toList();
        }
        model.addAttribute("products", products);
        model.addAttribute("q", query);
        model.addAttribute("type", type);
        return "products/list";
    }

    @GetMapping("/products/{id}")
    public String productDetail(@PathVariable Long id, Model model) {
        Product product = productRepository.findById(id)
                .filter(p -> Boolean.FALSE.equals(p.getIsDelete()) && p.getStatus() == Product.Status.AVAILABLE)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        model.addAttribute("product", product);
        return "products/detail";
    }
}


