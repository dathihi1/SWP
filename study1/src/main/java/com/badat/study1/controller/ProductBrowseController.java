package com.badat.study1.controller;

import com.badat.study1.model.Product;
import com.badat.study1.model.Review;
import com.badat.study1.model.Shop;
import com.badat.study1.model.Stall;
import com.badat.study1.model.User;
import com.badat.study1.model.Wallet;
import com.badat.study1.repository.ProductRepository;
import com.badat.study1.repository.ReviewRepository;
import com.badat.study1.repository.ShopRepository;
import com.badat.study1.repository.StallRepository;
import com.badat.study1.repository.WalletRepository;
import com.badat.study1.repository.WarehouseRepository;
import com.badat.study1.repository.UserRepository;
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
	private final ReviewRepository reviewRepository;
	private final ShopRepository shopRepository;
	private final StallRepository stallRepository;
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
		List<Stall> stalls;
        if (query != null && !query.isBlank()) {
            stalls = stallRepository.findByStallNameContainingIgnoreCaseAndIsDeleteFalseAndStatus(query.trim(), "OPEN");
        } else {
            stalls = stallRepository.findByStatusAndIsDeleteFalse("OPEN");
        }
        log.info("[Products] after base fetch (OPEN) stalls={} ", stalls.size());

		// filter by type
		if (type != null && !type.isBlank()) {
			String filterType = type.trim();
			stalls = stalls.stream()
					.filter(s -> filterType.equalsIgnoreCase(s.getStallCategory()))
					.toList();
            log.info("[Products] after type filter type='{}' -> {} stalls", filterType, stalls.size());
		}

        // preload shop map (exclude null values)
        java.util.Map<Long, Shop> shopMap = new java.util.HashMap<>();
        stalls.stream()
                .map(Stall::getShopId)
                .distinct()
                .forEach(id -> shopRepository.findById(id).ifPresent(shop -> shopMap.put(id, shop)));

		// filter by shop name
		if (shopName != null && !shopName.isBlank()) {
			String filterShop = shopName.trim().toLowerCase();
			stalls = stalls.stream()
					.filter(s -> {
						Shop shop = shopMap.get(s.getShopId());
						return shop != null && shop.getShopName() != null && shop.getShopName().toLowerCase().contains(filterShop);
					})
					.toList();
            log.info("[Products] after shop filter shop='{}' -> {} stalls", filterShop, stalls.size());
		}

		// compute ratings
		Map<Long, Double> stallRatings = stalls.stream().collect(Collectors.toMap(
				Stall::getId,
				s -> {
					Long stallId = s.getId();
					List<Review> reviews = reviewRepository.findByStallIdAndIsDeleteFalse(stallId);
					return reviews.stream()
							.map(Review::getRating)
							.filter(r -> r != null && r > 0)
							.mapToInt(Integer::intValue)
							.average()
							.orElse(0.0);
				}
		));

		// compute product counts
		Map<Long, Integer> productCounts = stalls.stream().collect(Collectors.toMap(
				Stall::getId,
				s -> (int) warehouseRepository.countAvailableItemsByStallId(s.getId())
		));

		// compute stock counts (same as available items)
		Map<Long, Integer> stockCounts = productCounts;

        // compute price ranges (min/max of available product prices)
        Map<Long, Map<String, BigDecimal>> priceRanges = stalls.stream().collect(Collectors.toMap(
                Stall::getId,
                s -> {
                    List<Product> products = productRepository.findByStallIdAndIsDeleteFalse(s.getId())
                            .stream()
                            .filter(p -> p.getStatus() == Product.Status.AVAILABLE && p.getPrice() != null)
                            .toList();
                    java.util.Map<String, BigDecimal> range = new java.util.HashMap<>();
                    if (products.isEmpty()) {
                        range.put("min", null);
                        range.put("max", null);
                        return range;
                    }
                    BigDecimal min = products.stream().map(Product::getPrice).min(BigDecimal::compareTo).orElse(null);
                    BigDecimal max = products.stream().map(Product::getPrice).max(BigDecimal::compareTo).orElse(null);
                    range.put("min", min);
                    range.put("max", max);
                    return range;
                }
        ));

		// filter by rating
		if (ratingMin != null) {
			double threshold = ratingMin;
			stalls = stalls.stream()
					.filter(s -> stallRatings.getOrDefault(s.getId(), 0.0) >= threshold)
					.toList();
            log.info("[Products] after ratingMin>={} -> {} stalls", threshold, stalls.size());
		}

		// filter by min product count
		if (productCountMin != null) {
			int minCount = productCountMin;
			stalls = stalls.stream()
					.filter(s -> productCounts.getOrDefault(s.getId(), 0) >= minCount)
					.toList();
            log.info("[Products] after productCountMin>={} -> {} stalls", minCount, stalls.size());
		}

		// filter by price range
		if (minPrice != null || maxPrice != null) {
			BigDecimal min = minPrice != null ? minPrice : BigDecimal.ZERO;
			BigDecimal max = maxPrice != null ? maxPrice : new BigDecimal("999999999");
			stalls = stalls.stream()
					.filter(s -> {
						List<Product> products = productRepository.findByStallIdAndIsDeleteFalse(s.getId());
						return products.stream()
								.filter(p -> p.getStatus() == Product.Status.AVAILABLE)
								.anyMatch(p -> p.getPrice() != null && p.getPrice().compareTo(min) >= 0 && p.getPrice().compareTo(max) <= 0);
					})
					.toList();
            log.info("[Products] after price range [{} - {}] -> {} stalls", min, max, stalls.size());
		}

		// sorting (name/products/rating/price)
        Map<Long, BigDecimal> minPricePerStall = stalls.stream().collect(Collectors.toMap(
                Stall::getId,
                s -> {
                    List<Product> products = productRepository.findByStallIdAndIsDeleteFalse(s.getId());
                    return products.stream()
                            .filter(p -> p.getStatus() == Product.Status.AVAILABLE && p.getPrice() != null)
                            .map(Product::getPrice)
                            .min(BigDecimal::compareTo)
                            .orElse(new BigDecimal("999999999"));
                }
        ));

		if (sortBy != null && !sortBy.isBlank()) {
			boolean asc = "asc".equalsIgnoreCase(order);
			java.util.Comparator<Stall> comparator;
			switch (sortBy.toLowerCase()) {
				case "products" -> comparator = java.util.Comparator.comparing(s -> productCounts.getOrDefault(s.getId(), 0));
				case "rating" -> comparator = java.util.Comparator.comparing(s -> stallRatings.getOrDefault(s.getId(), 0.0));
                case "price" -> comparator = java.util.Comparator.comparing(s -> minPricePerStall.getOrDefault(s.getId(), new BigDecimal("999999999")));
				case "name" -> comparator = java.util.Comparator.comparing(s -> s.getStallName() != null ? s.getStallName().toLowerCase() : "");
				default -> comparator = null;
			}
			if (comparator != null) {
				stalls = asc ? stalls.stream().sorted(comparator).toList() : stalls.stream().sorted(comparator.reversed()).toList();
                log.info("[Products] after sort by '{}' order='{}' -> {} stalls", sortBy, order, stalls.size());
			}
		}

        // categories for filter dropdown
        List<String> categories = stallRepository.findByStatusAndIsDeleteFalse("OPEN").stream()
				.map(Stall::getStallCategory)
				.filter(c -> c != null && !c.isBlank())
				.distinct()
				.sorted(String.CASE_INSENSITIVE_ORDER)
				.toList();

        // Pagination (in-memory)
        Pageable pageable = PageRequest.of(page, size);
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, stalls.size());
        List<Stall> paginatedStalls = startIndex >= stalls.size() ? List.of() : stalls.subList(startIndex, endIndex);
        int totalPages = stalls.isEmpty() ? 1 : (int) Math.ceil((double) stalls.size() / size);
        boolean hasPrev = page > 0;
        boolean hasNext = page < totalPages - 1;
        log.info("[Products] pagination page={}, size={}, start={}, end={}, total={}, totalPages={}, hasPrev={}, hasNext={}",
                page, size, startIndex, endIndex, stalls.size(), totalPages, hasPrev, hasNext);

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
        model.addAttribute("stalls", paginatedStalls);
		model.addAttribute("shopMap", shopMap);
		model.addAttribute("productCounts", productCounts);
		model.addAttribute("stockCounts", stockCounts);
		model.addAttribute("priceRanges", priceRanges);
		model.addAttribute("q", query);
		model.addAttribute("type", type);
		model.addAttribute("minPrice", minPrice);
		model.addAttribute("maxPrice", maxPrice);
		model.addAttribute("shop", shopName);
		model.addAttribute("stallRatings", stallRatings);
		model.addAttribute("categories", categories);
		model.addAttribute("ratingMin", ratingMin);
		model.addAttribute("productCountMin", productCountMin);
		model.addAttribute("sortBy", sortBy);
		model.addAttribute("order", order);
        // pagination attrs
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalElements", stalls.size());
        model.addAttribute("hasPrev", hasPrev);
        model.addAttribute("hasNext", hasNext);
		return "products/list";
	}

	@GetMapping({"/products/{id}", "/product/{id}"})
	public String productDetail(@PathVariable Long id, Model model) {
		Product product = productRepository.findById(id)
				.filter(p -> Boolean.FALSE.equals(p.getIsDelete()) && p.getStatus() == Product.Status.AVAILABLE)
				.orElseThrow(() -> new IllegalArgumentException("Product not found"));
		List<Review> reviews = reviewRepository.findByProductIdAndIsDeleteFalse(id);
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

		model.addAttribute("product", product);
		model.addAttribute("reviews", reviews);
		model.addAttribute("avgRating", avgRating);
		return "products/detail";
	}

	@GetMapping("/stall/{id}")
	public String stallDetail(@PathVariable Long id, Model model) {
		Stall stall = stallRepository.findById(id)
				.filter(s -> !s.isDelete() && "OPEN".equals(s.getStatus()))
				.orElseThrow(() -> new IllegalArgumentException("Stall not found or not available"));

		List<Product> products = productRepository.findByStallIdAndIsDeleteFalse(id)
				.stream()
				.filter(p -> p.getStatus() == Product.Status.AVAILABLE)
				.toList();

		// Tính tồn kho thực tế theo Warehouse cho từng sản phẩm trong stall (không sửa Product)
		java.util.Map<Long, Long> productStocks = products.stream()
			.collect(java.util.stream.Collectors.toMap(
				Product::getId,
                p -> warehouseRepository.countByProductIdAndLockedFalseAndIsDeleteFalse(p.getId())
			));

		Shop shop = shopRepository.findById(stall.getShopId()).orElse(null);

		// Determine seller username from shop owner
		String sellerUsername = null;
		try {
			if (shop != null && shop.getUserId() != null) {
				sellerUsername = userRepository.findById(shop.getUserId()).map(User::getUsername).orElse(null);
			}
		} catch (Exception ignored) {}

		List<Review> reviews = reviewRepository.findByStallIdAndIsDeleteFalse(id);
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

		model.addAttribute("stall", stall);
		model.addAttribute("products", products);
		model.addAttribute("productStocks", productStocks);
		model.addAttribute("shop", shop);
		model.addAttribute("sellerUsername", sellerUsername);
		model.addAttribute("reviews", reviews);
		model.addAttribute("avgRating", avgRating);
		return "stall/detail";
	}
}
