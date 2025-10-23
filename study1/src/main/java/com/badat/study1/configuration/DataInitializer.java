package com.badat.study1.configuration;

import com.badat.study1.model.*;
import com.badat.study1.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;
    private final BankAccountRepository bankAccountRepository;
    private final StallRepository stallRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Create sample users if not exists
        if (userRepository.count() == 0) {
            createSampleUsers();
        }
        
        // Create wallets for all users if not exists
        createWalletsForUsers();
        
        // Create sample bank accounts if not exists
        if (bankAccountRepository.count() == 0) {
            createSampleBankAccounts();
        }
        
        // Create sample shops if not exists
        if (shopRepository.count() == 0) {
            createSampleShops();
        }
        
        // Create sample stalls if not exists
        if (stallRepository.count() == 0) {
            createSampleStalls();
        }
        
        // Create sample products if not exists
        if (productRepository.count() == 0) {
            createSampleProducts();
        }
        
        log.info("Data initialization completed");
    }

    private void createSampleUsers() {
        User admin = User.builder()
                .username("admin")
                .email("admin@example.com")
                .password(passwordEncoder.encode("admin123"))
                .role(User.Role.ADMIN)
                .status(User.Status.ACTIVE)
                .build();
        userRepository.save(admin);

        User seller = User.builder()
                .username("seller1")
                .email("seller1@example.com")
                .password(passwordEncoder.encode("seller123"))
                .role(User.Role.SELLER)
                .status(User.Status.ACTIVE)
                .build();
        userRepository.save(seller);

        User customer = User.builder()
                .username("customer1")
                .email("customer1@example.com")
                .password(passwordEncoder.encode("customer123"))
                .role(User.Role.USER)
                .status(User.Status.ACTIVE)
                .build();
        userRepository.save(customer);
    }

    private void createWalletsForUsers() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            if (!walletRepository.findByUserId(user.getId()).isPresent()) {
                Wallet wallet = Wallet.builder()
                        .userId(user.getId())
                        .balance(BigDecimal.ZERO)
                        .build();
                walletRepository.save(wallet);
                log.info("Created wallet for user: {}", user.getUsername());
            }
        }
    }

    private void createSampleBankAccounts() {
        User seller = userRepository.findByUsername("seller1").orElse(null);
        if (seller != null) {
            BankAccount bankAccount = new BankAccount();
            bankAccount.setUserId(seller.getId());
            bankAccount.setBankName("Vietcombank");
            bankAccount.setAccountNumber("1234567890");
            bankAccount.setBankAccount("1234567890 Vietcombank");
            bankAccount.setCreatedAt(Instant.now());
            bankAccount.setDelete(false);
            bankAccountRepository.save(bankAccount);
        }
    }

    private void createSampleShops() {
        User seller = userRepository.findByUsername("seller1").orElse(null);
        List<BankAccount> bankAccounts = bankAccountRepository.findAll();
        if (seller == null) {
            log.warn("Sample seller not found, skipping shop creation");
            return;
        }
        if (bankAccounts.isEmpty()) {
            log.warn("No bank account found, skipping shop creation");
            return;
        }
        BankAccount bankAccount = bankAccounts.get(0);
        if (bankAccount != null) {
            Shop shop = new Shop();
            shop.setShopName("Tech Store");
            shop.setUserId(seller.getId());
            shop.setCccd("123456789");
            shop.setBankAccountId(bankAccount.getId());
            shop.setCreatedAt(Instant.now());
            shop.setIsDelete(false);
            shopRepository.save(shop);
        }
    }

    private void createSampleStalls() {
        List<Shop> shops = shopRepository.findAll();
        if (shops.isEmpty()) {
            log.warn("No shop found, skipping stall creation");
            return;
        }
        Shop shop = shops.get(0);
        if (shop != null) {
            Stall stall = new Stall();
            stall.setShopId(shop.getId());
            stall.setStallName("Digital Products Stall");
            stall.setBusinessType("Digital Services");
            stall.setStallCategory("Technology");
            stall.setDiscountPercentage(10.0);
            stall.setShortDescription("Premium digital products and services");
            stall.setDetailedDescription("We offer high-quality digital products including software licenses, email accounts, and digital certificates.");
            stall.setStatus("OPEN");
            stall.setCreatedAt(Instant.now());
            stall.setDelete(false);
            stallRepository.save(stall);
        }
    }

    private void createSampleProducts() {
        List<Shop> shops = shopRepository.findAll();
        List<Stall> stalls = stallRepository.findAll();
        if (shops.isEmpty() || stalls.isEmpty()) {
            log.warn("No shop or stall found, skipping product creation");
            return;
        }
        Shop shop = shops.get(0);
        Stall stall = stalls.get(0);
        
        // Email products
        List<Product> emailProducts = List.of(
                Product.builder()
                        .shopId(shop.getId())
                        .stallId(stall.getId())
                        .type("email")
                        .name("Gmail Premium 2024")
                        .description("Tài khoản Gmail premium với 15GB dung lượng")
                        .price(new BigDecimal("50000"))
                        .uniqueKey("gmail-premium-001")
                        .status(Product.Status.AVAILABLE)
                        .build(),
                Product.builder()
                        .shopId(shop.getId())
                        .stallId(stall.getId())
                        .type("email")
                        .name("Yahoo Mail Pro")
                        .description("Tài khoản Yahoo Mail chuyên nghiệp")
                        .price(new BigDecimal("30000"))
                        .uniqueKey("yahoo-pro-001")
                        .status(Product.Status.AVAILABLE)
                        .build()
        );

        // Software products
        List<Product> softwareProducts = List.of(
                Product.builder()
                        .shopId(shop.getId())
                        .stallId(stall.getId())
                        .type("software")
                        .name("Windows 11 Pro Key")
                        .description("Key bản quyền Windows 11 Professional")
                        .price(new BigDecimal("200000"))
                        .uniqueKey("win11-pro-001")
                        .status(Product.Status.AVAILABLE)
                        .build(),
                Product.builder()
                        .shopId(shop.getId())
                        .stallId(stall.getId())
                        .type("software")
                        .name("Office 365 License")
                        .description("Giấy phép Office 365 1 năm")
                        .price(new BigDecimal("150000"))
                        .uniqueKey("office365-001")
                        .status(Product.Status.AVAILABLE)
                        .build()
        );

        // Account products
        List<Product> accountProducts = List.of(
                Product.builder()
                        .shopId(shop.getId())
                        .stallId(stall.getId())
                        .type("account")
                        .name("Facebook Business Account")
                        .description("Tài khoản Facebook Business đã xác thực")
                        .price(new BigDecimal("100000"))
                        .uniqueKey("fb-business-001")
                        .status(Product.Status.AVAILABLE)
                        .build(),
                Product.builder()
                        .shopId(shop.getId())
                        .stallId(stall.getId())
                        .type("account")
                        .name("Instagram Verified Account")
                        .description("Tài khoản Instagram đã được xác thực")
                        .price(new BigDecimal("80000"))
                        .uniqueKey("ig-verified-001")
                        .status(Product.Status.AVAILABLE)
                        .build()
        );

        // Other products
        List<Product> otherProducts = List.of(
                Product.builder()
                        .shopId(shop.getId())
                        .stallId(stall.getId())
                        .type("other")
                        .name("Domain .com Premium")
                        .description("Tên miền .com cao cấp")
                        .price(new BigDecimal("500000"))
                        .uniqueKey("domain-com-001")
                        .status(Product.Status.AVAILABLE)
                        .build(),
                Product.builder()
                        .shopId(shop.getId())
                        .stallId(stall.getId())
                        .type("other")
                        .name("SSL Certificate")
                        .description("Chứng chỉ SSL bảo mật")
                        .price(new BigDecimal("200000"))
                        .uniqueKey("ssl-cert-001")
                        .status(Product.Status.AVAILABLE)
                        .build()
        );

        productRepository.saveAll(emailProducts);
        productRepository.saveAll(softwareProducts);
        productRepository.saveAll(accountProducts);
        productRepository.saveAll(otherProducts);
    }
}
