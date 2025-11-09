package com.badat.study1.controller;

import com.badat.study1.model.Product;
import com.badat.study1.model.Review;
import com.badat.study1.model.Shop;
import com.badat.study1.model.User;
import com.badat.study1.model.Wallet;
import com.badat.study1.repository.ProductRepository;
import com.badat.study1.repository.ProductVariantRepository;
import com.badat.study1.repository.ReviewRepository;
import com.badat.study1.repository.ShopRepository;
import com.badat.study1.repository.WalletRepository;
import com.badat.study1.repository.WarehouseRepository;
import com.badat.study1.repository.UserRepository;
import com.badat.study1.model.ProductVariant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ProductBrowseController {

    private static final Logger log = LoggerFactory.getLogger(ProductBrowseController.class);
	private final ProductRepository productRepository;
	private final ProductVariantRepository productVariantRepository;
	private final ReviewRepository reviewRepository;
	private final ShopRepository shopRepository;
	private final WalletRepository walletRepository;
    private final WarehouseRepository warehouseRepository;
    private final UserRepository userRepository;

	@GetMapping("/products")
    public String listProducts(
			@RequestParam(value = "q", required = false) String query,
			@RequestParam(value = "type", required = false) String type,
			@RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
			@RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
			@RequestParam(value = "shop", required = false) String shopName,
			@RequestParam(value = "ratingMin", required = false) Double ratingMin,
			@RequestParam(value = "productCountMin", required = false) Integer productCountMin,
			@RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "order", required = false, defaultValue = "desc") String order,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "12") int size,
			Model model) {
        log.info("[Products] params q='{}', type='{}', shop='{}', ratingMin={}, productCountMin={}, sortBy='{}', order='{}', page={}, size={}",
                query, type, shopName, ratingMin, productCountMin, sortBy, order, page, size);
		List<Product> products;
        if (query != null && !query.isBlank()) {
            products = productRepository.findByProductNameContainingIgnoreCaseAndIsDeleteFalseAndStatus(query.trim(), "OPEN");
        } else {
            products = productRepository.findByStatusAndIsDeleteFalse("OPEN");
        }
        log.info("[Products] after base fetch (OPEN) products={} ", products.size());

		// filter by type
		if (type != null && !type.isBlank()) {
			String filterType = type.trim();
			products = products.stream()
					.filter(p -> filterType.equalsIgnoreCase(p.getProductCategory()))
					.toList();
            log.info("[Products] after type filter type='{}' -> {} products", filterType, products.size());
		}

        // preload shop map (exclude null values)
        java.util.Map<Long, Shop> shopMap = new java.util.HashMap<>();
        products.stream()
                .map(Product::getShopId)
                .distinct()
                .forEach(id -> shopRepository.findById(id).ifPresent(shop -> shopMap.put(id, shop)));

		// filter by shop name
		if (shopName != null && !shopName.isBlank()) {
			String filterShop = shopName.trim().toLowerCase();
			products = products.stream()
					.filter(p -> {
						Shop shop = shopMap.get(p.getShopId());
						return shop != null && shop.getShopName() != null && shop.getShopName().toLowerCase().contains(filterShop);
					})
					.toList();
            log.info("[Products] after shop filter shop='{}' -> {} products", filterShop, products.size());
		}

		// compute ratings
		Map<Long, Double> productRatings = products.stream().collect(Collectors.toMap(
				Product::getId,
				p -> {
					Long productId = p.getId();
					List<Review> reviews = reviewRepository.findByProductIdAndIsDeleteFalse(productId);
					return reviews.stream()
							.map(Review::getRating)
							.filter(r -> r != null && r > 0)
							.mapToInt(Integer::intValue)
							.average()
							.orElse(0.0);
				}
		));

		// compute product variant counts
		Map<Long, Integer> productVariantCounts = products.stream().collect(Collectors.toMap(
				Product::getId,
				p -> (int) warehouseRepository.countAvailableItemsByProductId(p.getId())
		));

		// compute stock counts (same as available items)
		Map<Long, Integer> stockCounts = productVariantCounts;

        // compute price ranges (min/max of available product variant prices)
        Map<Long, Map<String, BigDecimal>> priceRanges = products.stream().collect(Collectors.toMap(
                Product::getId,
                p -> {
                    List<ProductVariant> productVariants = productVariantRepository.findByProductIdAndIsDeleteFalse(p.getId())
                            .stream()
                            .filter(pv -> pv.getStatus() == ProductVariant.Status.AVAILABLE && pv.getPrice() != null)
                            .toList();
                    java.util.Map<String, BigDecimal> range = new java.util.HashMap<>();
                    if (productVariants.isEmpty()) {
                        range.put("min", null);
                        range.put("max", null);
                        return range;
                    }
                    BigDecimal min = productVariants.stream().map(ProductVariant::getPrice).min(BigDecimal::compareTo).orElse(null);
                    BigDecimal max = productVariants.stream().map(ProductVariant::getPrice).max(BigDecimal::compareTo).orElse(null);
                    range.put("min", min);
                    range.put("max", max);
                    return range;
                }
        ));

		// filter by rating
		if (ratingMin != null) {
			double threshold = ratingMin;
			products = products.stream()
					.filter(p -> productRatings.getOrDefault(p.getId(), 0.0) >= threshold)
					.toList();
            log.info("[Products] after ratingMin>={} -> {} products", threshold, products.size());
		}

		// filter by min product variant count
		if (productCountMin != null) {
			int minCount = productCountMin;
			products = products.stream()
					.filter(p -> productVariantCounts.getOrDefault(p.getId(), 0) >= minCount)
					.toList();
            log.info("[Products] after productCountMin>={} -> {} products", minCount, products.size());
		}

		// filter by price range
		if (minPrice != null || maxPrice != null) {
			BigDecimal min = minPrice != null ? minPrice : BigDecimal.ZERO;
			BigDecimal max = maxPrice != null ? maxPrice : new BigDecimal("999999999");
			products = products.stream()
					.filter(p -> {
						List<ProductVariant> productVariants = productVariantRepository.findByProductIdAndIsDeleteFalse(p.getId());
						return productVariants.stream()
								.filter(pv -> pv.getStatus() == ProductVariant.Status.AVAILABLE)
								.anyMatch(pv -> pv.getPrice() != null && pv.getPrice().compareTo(min) >= 0 && pv.getPrice().compareTo(max) <= 0);
					})
					.toList();
            log.info("[Products] after price range [{} - {}] -> {} products", min, max, products.size());
		}

		// sorting (name/products/rating/price)
        Map<Long, BigDecimal> minPricePerProduct = products.stream().collect(Collectors.toMap(
                Product::getId,
                p -> {
                    List<ProductVariant> productVariants = productVariantRepository.findByProductIdAndIsDeleteFalse(p.getId());
                    return productVariants.stream()
                            .filter(pv -> pv.getStatus() == ProductVariant.Status.AVAILABLE && pv.getPrice() != null)
                            .map(ProductVariant::getPrice)
                            .min(BigDecimal::compareTo)
                            .orElse(new BigDecimal("999999999"));
                }
        ));

		if (sortBy != null && !sortBy.isBlank()) {
			boolean asc = "asc".equalsIgnoreCase(order);
			java.util.Comparator<Product> comparator;
			switch (sortBy.toLowerCase()) {
				case "products" -> comparator = java.util.Comparator.comparing(p -> productVariantCounts.getOrDefault(p.getId(), 0));
				case "rating" -> comparator = java.util.Comparator.comparing(p -> productRatings.getOrDefault(p.getId(), 0.0));
                case "price" -> comparator = java.util.Comparator.comparing(p -> minPricePerProduct.getOrDefault(p.getId(), new BigDecimal("999999999")));
				case "name" -> comparator = java.util.Comparator.comparing(p -> p.getProductName() != null ? p.getProductName().toLowerCase() : "");
				default -> comparator = null;
			}
			if (comparator != null) {
				products = asc ? products.stream().sorted(comparator).toList() : products.stream().sorted(comparator.reversed()).toList();
                log.info("[Products] after sort by '{}' order='{}' -> {} products", sortBy, order, products.size());
			}
		}

        // categories for filter dropdown
        List<String> categories = productRepository.findByStatusAndIsDeleteFalse("OPEN").stream()
				.map(Product::getProductCategory)
				.filter(c -> c != null && !c.isBlank())
				.distinct()
				.sorted(String.CASE_INSENSITIVE_ORDER)
				.toList();

        // Pagination (in-memory)
        Pageable pageable = PageRequest.of(page, size);
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, products.size());
        List<Product> paginatedProducts = startIndex >= products.size() ? List.of() : products.subList(startIndex, endIndex);
        int totalPages = products.isEmpty() ? 1 : (int) Math.ceil((double) products.size() / size);
        boolean hasPrev = page > 0;
        boolean hasNext = page < totalPages - 1;
        log.info("[Products] pagination page={}, size={}, start={}, end={}, total={}, totalPages={}, hasPrev={}, hasNext={}",
                page, size, startIndex, endIndex, products.size(), totalPages, hasPrev, hasNext);

		// auth info
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		boolean isAuthenticated = authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName());
		model.addAttribute("isAuthenticated", isAuthenticated);
		model.addAttribute("walletBalance", BigDecimal.ZERO);
		if (isAuthenticated) {
			Object principal = authentication.getPrincipal();
			if (principal instanceof User user) {
				model.addAttribute("username", user.getUsername());
				model.addAttribute("userRole", user.getRole().name());
				BigDecimal walletBalance = walletRepository.findByUserId(user.getId()).map(Wallet::getBalance).orElse(BigDecimal.ZERO);
				model.addAttribute("walletBalance", walletBalance);
			} else {
				model.addAttribute("username", authentication.getName());
				model.addAttribute("userRole", "USER");
			}
		}

        // model attributes
        model.addAttribute("stalls", paginatedProducts);
		model.addAttribute("shopMap", shopMap);
		model.addAttribute("productCounts", productVariantCounts);
		model.addAttribute("stockCounts", stockCounts);
		model.addAttribute("priceRanges", priceRanges);
		model.addAttribute("q", query);
		model.addAttribute("type", type);
		model.addAttribute("minPrice", minPrice);
		model.addAttribute("maxPrice", maxPrice);
		model.addAttribute("shop", shopName);
		model.addAttribute("stallRatings", productRatings);
		model.addAttribute("categories", categories);
		model.addAttribute("ratingMin", ratingMin);
		model.addAttribute("productCountMin", productCountMin);
		model.addAttribute("sortBy", sortBy);
		model.addAttribute("order", order);
        // pagination attrs
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalElements", products.size());
        model.addAttribute("hasPrev", hasPrev);
        model.addAttribute("hasNext", hasNext);
		return "products/list";
	}

    @GetMapping({"/product-variant/{id}", "/product/{id}"})
	public String productDetail(@PathVariable Long id, Model model) {
		ProductVariant productVariant = productVariantRepository.findById(id)
				.filter(pv -> Boolean.FALSE.equals(pv.getIsDelete()) && pv.getStatus() == ProductVariant.Status.AVAILABLE)
				.orElseThrow(() -> new IllegalArgumentException("Product variant not found"));
		List<Review> reviews = reviewRepository.findByProductVariantIdAndIsDeleteFalse(productVariant.getId());
		double avgRating = reviews.stream()
				.map(Review::getRating)
				.filter(r -> r != null && r > 0)
				.mapToInt(Integer::intValue)
				.average()
				.orElse(0.0);

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		boolean isAuthenticated = authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName());
		model.addAttribute("isAuthenticated", isAuthenticated);
		model.addAttribute("walletBalance", BigDecimal.ZERO);
		if (isAuthenticated) {
			Object principal = authentication.getPrincipal();
			if (principal instanceof User user) {
				model.addAttribute("username", user.getUsername());
				model.addAttribute("userRole", user.getRole().name());
				BigDecimal walletBalance = walletRepository.findByUserId(user.getId()).map(Wallet::getBalance).orElse(BigDecimal.ZERO);
				model.addAttribute("walletBalance", walletBalance);
			} else {
				model.addAttribute("username", authentication.getName());
				model.addAttribute("userRole", "USER");
			}
		}

		model.addAttribute("product", productVariant);
		model.addAttribute("reviews", reviews);
		model.addAttribute("avgRating", avgRating);
		return "products/detail";
	}

    @GetMapping("/products/{id}")
    public String stallDetail(@PathVariable("id") Long productId, Model model) {
        Product product = productRepository.findById(productId)
				.filter(p -> !p.isDelete() && "OPEN".equals(p.getStatus()))
				.orElseThrow(() -> new IllegalArgumentException("Product not found or not available"));

        List<ProductVariant> productVariants = productVariantRepository.findByProductIdAndIsDeleteFalse(productId)
				.stream()
				.filter(pv -> pv.getStatus() == ProductVariant.Status.AVAILABLE)
				.toList();

		// Tính tồn kho thực tế theo Warehouse cho từng product variant
		java.util.Map<Long, Long> productVariantStocks = productVariants.stream()
			.collect(java.util.stream.Collectors.toMap(
				ProductVariant::getId,
                pv -> warehouseRepository.countByProductVariantIdAndLockedFalseAndIsDeleteFalse(pv.getId())
			));

		Shop shop = shopRepository.findById(product.getShopId()).orElse(null);

		// Determine seller username from shop owner
		String sellerUsername = null;
		try {
			if (shop != null && shop.getUserId() != null) {
				sellerUsername = userRepository.findById(shop.getUserId()).map(User::getUsername).orElse(null);
			}
		} catch (Exception ignored) {}

        List<Review> reviews = reviewRepository.findByProductIdAndIsDeleteFalse(productId);
		double avgRating = reviews.stream()
				.map(Review::getRating)
				.filter(r -> r != null && r > 0)
				.mapToInt(Integer::intValue)
				.average()
				.orElse(0.0);

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		boolean isAuthenticated = authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName());
		model.addAttribute("isAuthenticated", isAuthenticated);
		model.addAttribute("walletBalance", BigDecimal.ZERO);
		if (isAuthenticated) {
			Object principal = authentication.getPrincipal();
			if (principal instanceof User user) {
				model.addAttribute("username", user.getUsername());
				model.addAttribute("userRole", user.getRole().name());
				BigDecimal walletBalance = walletRepository.findByUserId(user.getId()).map(Wallet::getBalance).orElse(BigDecimal.ZERO);
				model.addAttribute("walletBalance", walletBalance);
			} else {
				model.addAttribute("username", authentication.getName());
				model.addAttribute("userRole", "USER");
			}
		}

		model.addAttribute("stall", product);
		model.addAttribute("products", productVariants);
		model.addAttribute("productStocks", productVariantStocks);
		model.addAttribute("shop", shop);
		model.addAttribute("sellerUsername", sellerUsername);
		model.addAttribute("reviews", reviews);
		model.addAttribute("avgRating", avgRating);
        return "products/detail";
	}
}
