package com.badat.study1.admin.service;

import com.badat.study1.admin.util.AdminBeanUtil;
import com.badat.study1.repository.ProductRepository;
import com.badat.study1.repository.ShopRepository;
import com.badat.study1.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminModerationService {
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;

    public AdminModerationService(UserRepository userRepository,
                                  ShopRepository shopRepository,
                                  ProductRepository productRepository) {
        this.userRepository = userRepository;
        this.shopRepository = shopRepository;
        this.productRepository = productRepository;
    }

    /* USER */
    @Transactional
    public void activateUser(Long userId, boolean active) {
        var u = userRepository.findById(userId).orElseThrow();
        // thử lần lượt enabled/active/banned (đảo ngược nếu cần)
        AdminBeanUtil.setBoolean(u, "enabled", "active", String.valueOf(active));
        if (!active) AdminBeanUtil.setBoolean(u, "banned", "true");
        userRepository.save(u);
    }

    /* SHOP */
    @Transactional
    public void hideShop(Long shopId, boolean hidden) {
        var s = shopRepository.findById(shopId).orElseThrow();
        AdminBeanUtil.setBoolean(s, "hidden", "isHidden", String.valueOf(hidden));
        shopRepository.save(s);
    }

    @Transactional
    public void deleteShop(Long shopId) {
        // nếu có soft-delete: AdminBeanUtil.setBoolean(s,"deleted","true"); else hard delete
        shopRepository.deleteById(shopId);
    }

    /* PRODUCT */
    @Transactional
    public void hideProduct(Long productId, boolean hidden) {
        var p = productRepository.findById(productId).orElseThrow();
        AdminBeanUtil.setBoolean(p, "hidden", "isHidden", String.valueOf(hidden));
        productRepository.save(p);
    }

    @Transactional
    public void deleteProduct(Long productId) {
        productRepository.deleteById(productId);
    }
}
