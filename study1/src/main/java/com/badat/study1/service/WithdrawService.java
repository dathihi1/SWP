package com.badat.study1.service;

import com.badat.study1.dto.request.WithdrawRequestDto;
import com.badat.study1.dto.response.WithdrawRequestResponse;
import com.badat.study1.model.Shop;
import com.badat.study1.model.User;
import com.badat.study1.model.Wallet;
import com.badat.study1.model.WalletHistory;
import com.badat.study1.model.WithdrawRequest;
import com.badat.study1.repository.ShopRepository;
import com.badat.study1.repository.WalletHistoryRepository;
import com.badat.study1.repository.WalletRepository;
import com.badat.study1.repository.WithdrawRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawService {
    
    private final WithdrawRequestRepository withdrawRequestRepository;
    private final ShopRepository shopRepository;
    private final WalletRepository walletRepository;
    private final WalletHistoryRepository walletHistoryRepository;
    private final RedisLockRegistry redisLockRegistry;
    
    @Transactional
    public WithdrawRequestResponse createWithdrawRequest(WithdrawRequestDto requestDto) {
        // Get current user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();
        
        // Find user's shop
        Shop shop = shopRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Bạn chưa có gian hàng"));
        
        // User-level lock để tránh race condition khi cùng 1 user tạo multiple withdraw requests
        String userWithdrawLockKey = "user:withdraw:lock:" + user.getId();
        Lock userWithdrawLock = redisLockRegistry.obtain(userWithdrawLockKey);
        
        try {
            if (userWithdrawLock.tryLock(10, TimeUnit.SECONDS)) {
                log.info("Acquired user withdraw lock for user: {}", user.getId());
                
                // Get user's wallet trong lock để đảm bảo consistency
                Wallet wallet = walletRepository.findByUserId(user.getId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy ví của bạn"));
                
                // Validate amount
                if (requestDto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new RuntimeException("Số tiền phải lớn hơn 0");
                }
                
                // Minimum withdraw amount (100,000 VND)
                BigDecimal minimumAmount = new BigDecimal("100000");
                if (requestDto.getAmount().compareTo(minimumAmount) < 0) {
                    throw new RuntimeException("Số tiền rút tối thiểu là 100,000 VNĐ");
                }
                
                // Check balance trong lock để tránh race condition
                if (requestDto.getAmount().compareTo(wallet.getBalance()) > 0) {
                    throw new RuntimeException("Số tiền rút không được vượt quá số dư hiện có: " + wallet.getBalance() + " VNĐ");
                }
                
                // Validate bank account information
                if (requestDto.getBankAccountNumber() == null || requestDto.getBankAccountNumber().trim().isEmpty()) {
                    throw new RuntimeException("Số tài khoản ngân hàng không được để trống");
                }
                
                if (requestDto.getBankAccountName() == null || requestDto.getBankAccountName().trim().isEmpty()) {
                    throw new RuntimeException("Tên chủ tài khoản không được để trống");
                }
                
                if (requestDto.getBankName() == null || requestDto.getBankName().trim().isEmpty()) {
                    throw new RuntimeException("Tên ngân hàng không được để trống");
                }
                
                // Check for pending withdraw requests trong lock
                List<WithdrawRequest> pendingRequests = withdrawRequestRepository.findByShopIdAndStatus(shop.getId(), WithdrawRequest.Status.PENDING);
                if (!pendingRequests.isEmpty()) {
                    throw new RuntimeException("Bạn đã có yêu cầu rút tiền đang chờ duyệt. Vui lòng chờ admin xử lý yêu cầu trước đó.");
                }
                
                // Create withdraw request
                WithdrawRequest withdrawRequest = WithdrawRequest.builder()
                        .shopId(shop.getId())
                        .amount(requestDto.getAmount())
                        .bankAccountNumber(requestDto.getBankAccountNumber())
                        .bankAccountName(requestDto.getBankAccountName())
                        .bankName(requestDto.getBankName())
                        .note(requestDto.getNote())
                        .status(WithdrawRequest.Status.PENDING)
                        .build();
                
                withdrawRequest = withdrawRequestRepository.save(withdrawRequest);
                
                // Hold the amount from wallet (subtract from available balance) trong lock
                BigDecimal newBalance = wallet.getBalance().subtract(requestDto.getAmount());
                wallet.setBalance(newBalance);
                walletRepository.save(wallet);
                
                // Create wallet history record for the hold
                WalletHistory walletHistory = WalletHistory.builder()
                        .walletId(wallet.getId())
                        .amount(requestDto.getAmount())
                        .type(WalletHistory.Type.WITHDRAW)
                        .status(WalletHistory.Status.PENDING)
                        .description("Tạm giữ tiền cho yêu cầu rút tiền #" + withdrawRequest.getId())
                        .referenceId(withdrawRequest.getId().toString())
                        .isDelete(false)
                        .createdBy(user.getId().toString())
                        .createdAt(java.time.Instant.now())
                        .build();
                walletHistoryRepository.save(walletHistory);
                
                log.info("Created withdraw request: {} for user: {} with amount: {}. Amount held from wallet.", 
                        withdrawRequest.getId(), user.getUsername(), requestDto.getAmount());
                
                return WithdrawRequestResponse.fromEntity(withdrawRequest);
                
            } else {
                log.warn("Failed to acquire user withdraw lock for user: {} - another withdraw request is in progress", user.getId());
                throw new RuntimeException("Bạn đang có yêu cầu rút tiền đang được xử lý. Vui lòng đợi hoàn tất trước khi tạo yêu cầu mới.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Bị gián đoạn khi tạo yêu cầu rút tiền", e);
        } finally {
            userWithdrawLock.unlock();
            log.info("Released user withdraw lock for user: {}", user.getId());
        }
    }
    
    public List<WithdrawRequestResponse> getWithdrawRequestsByUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();
        
        Shop shop = shopRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Bạn chưa có gian hàng"));
        
        List<WithdrawRequest> requests = withdrawRequestRepository.findByShopIdOrderByCreatedAtDesc(shop.getId());
        
        return requests.stream()
                .map(WithdrawRequestResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    public List<WithdrawRequestResponse> getWithdrawRequestsByUserWithFilters(
            String startDate, String endDate, String status, String minAmount, String maxAmount,
            String bankAccountNumber, String bankName, String bankAccountName) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();
        
        Shop shop = shopRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Bạn chưa có gian hàng"));
        
        // Convert String status to enum
        WithdrawRequest.Status statusEnum = null;
        if (status != null && !status.isEmpty()) {
            try {
                statusEnum = WithdrawRequest.Status.valueOf(status);
            } catch (IllegalArgumentException e) {
                // Invalid status, ignore filter
            }
        }
        
        List<WithdrawRequest> requests = withdrawRequestRepository.findByShopIdWithFilters(
                shop.getId(), startDate, endDate, statusEnum, minAmount, maxAmount,
                bankAccountNumber, bankName, bankAccountName);
        
        return requests.stream()
                .map(WithdrawRequestResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    public List<WithdrawRequestResponse> getAllPendingWithdrawRequests() {
        // Check if current user is admin
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) auth.getPrincipal();
        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Chỉ admin mới có thể xem tất cả yêu cầu rút tiền");
        }
        
        List<WithdrawRequest> requests = withdrawRequestRepository.findByStatusOrderByCreatedAtDesc(WithdrawRequest.Status.PENDING);
        
        return requests.stream()
                .map(request -> {
                    WithdrawRequestResponse response = WithdrawRequestResponse.fromEntity(request);
                    // Add shop name
                    try {
                        Shop shop = shopRepository.findById(request.getShopId()).orElse(null);
                        if (shop != null) {
                            response.setShopName(shop.getShopName());
                        }
                    } catch (Exception e) {
                        log.warn("Could not load shop name for request {}: {}", request.getId(), e.getMessage());
                    }
                    return response;
                })
                .collect(Collectors.toList());
    }
    
    @Transactional
    public void approveWithdrawRequest(Long requestId) {
        // Check if current user is admin
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) auth.getPrincipal();
        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Chỉ admin mới có thể duyệt yêu cầu rút tiền");
        }
        
        WithdrawRequest request = withdrawRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu rút tiền"));
        
        if (request.getStatus() != WithdrawRequest.Status.PENDING) {
            throw new RuntimeException("Yêu cầu này đã được xử lý");
        }
        
        // Get shop and user
        Shop shop = shopRepository.findById(request.getShopId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy gian hàng"));
        
        Wallet wallet = walletRepository.findByUserId(shop.getUserId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví"));
        
        // Update withdraw request status
        request.setStatus(WithdrawRequest.Status.APPROVED);
        withdrawRequestRepository.save(request);
        
        // Update wallet history from PENDING to SUCCESS
        walletHistoryRepository.findByWalletIdAndReferenceIdAndTypeAndStatus(
                wallet.getId(), 
                request.getId().toString(), 
                WalletHistory.Type.WITHDRAW, 
                WalletHistory.Status.PENDING
        ).ifPresent(walletHistory -> {
            walletHistory.setStatus(WalletHistory.Status.SUCCESS);
            walletHistory.setDescription("Rút tiền thành công từ yêu cầu #" + request.getId() + " - " + request.getBankName() + " - " + request.getBankAccountNumber());
            walletHistory.setUpdatedAt(java.time.Instant.now());
            walletHistoryRepository.save(walletHistory);
        });
        
        log.info("Admin {} approved withdraw request: {} for amount: {} VND to bank account: {} - {}", 
                currentUser.getUsername(), requestId, request.getAmount(), request.getBankName(), request.getBankAccountNumber());
    }
    
    @Transactional
    public void rejectWithdrawRequest(Long requestId) {
        // Check if current user is admin
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) auth.getPrincipal();
        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Chỉ admin mới có thể từ chối yêu cầu rút tiền");
        }
        
        WithdrawRequest request = withdrawRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu rút tiền"));
        
        if (request.getStatus() != WithdrawRequest.Status.PENDING) {
            throw new RuntimeException("Yêu cầu này đã được xử lý");
        }
        
        // Get shop and user
        Shop shop = shopRepository.findById(request.getShopId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy gian hàng"));
        
        Wallet wallet = walletRepository.findByUserId(shop.getUserId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví"));
        
        // Update withdraw request status
        request.setStatus(WithdrawRequest.Status.REJECTED);
        withdrawRequestRepository.save(request);
        
        // Return the held amount to wallet
        BigDecimal newBalance = wallet.getBalance().add(request.getAmount());
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);
        
        // Update wallet history from PENDING to FAILED
        walletHistoryRepository.findByWalletIdAndReferenceIdAndTypeAndStatus(
                wallet.getId(), 
                request.getId().toString(), 
                WalletHistory.Type.WITHDRAW, 
                WalletHistory.Status.PENDING
        ).ifPresent(walletHistory -> {
            walletHistory.setStatus(WalletHistory.Status.FAILED);
            walletHistory.setDescription("Yêu cầu rút tiền #" + request.getId() + " bị từ chối - Tiền đã được hoàn trả");
            walletHistory.setUpdatedAt(java.time.Instant.now());
            walletHistoryRepository.save(walletHistory);
        });
        
        log.info("Admin {} rejected withdraw request: {} and returned amount: {} VND to wallet. Bank account: {} - {}", 
                currentUser.getUsername(), requestId, request.getAmount(), request.getBankName(), request.getBankAccountNumber());
    }
    
    // Simple methods without authentication for admin-simple page
    public List<WithdrawRequestResponse> getAllPendingWithdrawRequestsSimple() {
        List<WithdrawRequest> requests = withdrawRequestRepository.findByStatusOrderByCreatedAtDesc(WithdrawRequest.Status.PENDING);
        
        return requests.stream()
                .map(request -> {
                    WithdrawRequestResponse response = WithdrawRequestResponse.fromEntity(request);
                    // Add shop name
                    try {
                        Shop shop = shopRepository.findById(request.getShopId()).orElse(null);
                        if (shop != null) {
                            response.setShopName(shop.getShopName());
                        }
                    } catch (Exception e) {
                        log.warn("Could not load shop name for request {}: {}", request.getId(), e.getMessage());
                    }
                    return response;
                })
                .collect(Collectors.toList());
    }
    
    @Transactional
    public void approveWithdrawRequestSimple(Long requestId) {
        WithdrawRequest request = withdrawRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu rút tiền"));
        
        if (request.getStatus() != WithdrawRequest.Status.PENDING) {
            throw new RuntimeException("Yêu cầu này đã được xử lý");
        }
        
        // Get shop and user
        Shop shop = shopRepository.findById(request.getShopId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy gian hàng"));
        
        Wallet wallet = walletRepository.findByUserId(shop.getUserId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví"));
        
        // Update withdraw request status
        request.setStatus(WithdrawRequest.Status.APPROVED);
        withdrawRequestRepository.save(request);
        
        // Update wallet history from PENDING to SUCCESS
        walletHistoryRepository.findByWalletIdAndReferenceIdAndTypeAndStatus(
                wallet.getId(), 
                request.getId().toString(), 
                WalletHistory.Type.WITHDRAW, 
                WalletHistory.Status.PENDING
        ).ifPresent(walletHistory -> {
            walletHistory.setStatus(WalletHistory.Status.SUCCESS);
            walletHistory.setDescription("Rút tiền thành công từ yêu cầu #" + request.getId() + " - " + request.getBankName() + " - " + request.getBankAccountNumber());
            walletHistory.setUpdatedAt(java.time.Instant.now());
            walletHistoryRepository.save(walletHistory);
        });
        
        log.info("Simple admin approved withdraw request: {} for amount: {} VND to bank account: {} - {}", 
                requestId, request.getAmount(), request.getBankName(), request.getBankAccountNumber());
    }
    
    @Transactional
    public void rejectWithdrawRequestSimple(Long requestId) {
        WithdrawRequest request = withdrawRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu rút tiền"));
        
        if (request.getStatus() != WithdrawRequest.Status.PENDING) {
            throw new RuntimeException("Yêu cầu này đã được xử lý");
        }
        
        // Get shop and user
        Shop shop = shopRepository.findById(request.getShopId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy gian hàng"));
        
        Wallet wallet = walletRepository.findByUserId(shop.getUserId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví"));
        
        // Update withdraw request status
        request.setStatus(WithdrawRequest.Status.REJECTED);
        withdrawRequestRepository.save(request);
        
        // Return the held amount to wallet
        BigDecimal newBalance = wallet.getBalance().add(request.getAmount());
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);
        
        // Update wallet history from PENDING to FAILED
        walletHistoryRepository.findByWalletIdAndReferenceIdAndTypeAndStatus(
                wallet.getId(), 
                request.getId().toString(), 
                WalletHistory.Type.WITHDRAW, 
                WalletHistory.Status.PENDING
        ).ifPresent(walletHistory -> {
            walletHistory.setStatus(WalletHistory.Status.FAILED);
            walletHistory.setDescription("Yêu cầu rút tiền #" + request.getId() + " bị từ chối - Tiền đã được hoàn trả");
            walletHistory.setUpdatedAt(java.time.Instant.now());
            walletHistoryRepository.save(walletHistory);
        });
        
        log.info("Simple admin rejected withdraw request: {} and returned amount: {} VND to wallet. Bank account: {} - {}", 
                requestId, request.getAmount(), request.getBankName(), request.getBankAccountNumber());
    }
    
    public List<WithdrawRequestResponse> getWithdrawRequestsByStatus(WithdrawRequest.Status status) {
        List<WithdrawRequest> requests = withdrawRequestRepository.findByStatus(status);
        return requests.stream()
                .map(request -> {
                    WithdrawRequestResponse response = WithdrawRequestResponse.fromEntity(request);
                    // Add shop name
                    try {
                        Shop shop = shopRepository.findById(request.getShopId()).orElse(null);
                        if (shop != null) {
                            response.setShopName(shop.getShopName());
                        }
                    } catch (Exception e) {
                        log.warn("Could not load shop name for request {}: {}", request.getId(), e.getMessage());
                    }
                    return response;
                })
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())) // Sort by newest first
                .collect(java.util.stream.Collectors.toList());
    }
    
    public List<WithdrawRequestResponse> filterWithdrawRequests(
            List<WithdrawRequestResponse> requests,
            String dateFrom,
            String dateTo,
            String searchName,
            String searchAccount,
            String searchBank) {
        
        return requests.stream()
                .filter(request -> {
                    // Date filter
                    if (dateFrom != null && !dateFrom.trim().isEmpty()) {
                        try {
                            LocalDate fromDate = LocalDate.parse(dateFrom);
                            LocalDate requestDate = request.getCreatedAt().toLocalDate();
                            if (requestDate.isBefore(fromDate)) {
                                return false;
                            }
                        } catch (Exception e) {
                            log.warn("Invalid dateFrom format: {}", dateFrom);
                        }
                    }
                    
                    if (dateTo != null && !dateTo.trim().isEmpty()) {
                        try {
                            LocalDate toDate = LocalDate.parse(dateTo);
                            LocalDate requestDate = request.getCreatedAt().toLocalDate();
                            if (requestDate.isAfter(toDate)) {
                                return false;
                            }
                        } catch (Exception e) {
                            log.warn("Invalid dateTo format: {}", dateTo);
                        }
                    }
                    
                    // Name search (search in shop name)
                    if (searchName != null && !searchName.trim().isEmpty()) {
                        String shopName = request.getShopName();
                        if (shopName == null || !shopName.toLowerCase().contains(searchName.toLowerCase())) {
                            return false;
                        }
                    }
                    
                    // Account number search
                    if (searchAccount != null && !searchAccount.trim().isEmpty()) {
                        String accountNumber = request.getBankAccountNumber();
                        if (accountNumber == null || !accountNumber.contains(searchAccount)) {
                            return false;
                        }
                    }
                    
                    // Bank name search
                    if (searchBank != null && !searchBank.trim().isEmpty()) {
                        String bankName = request.getBankName();
                        if (bankName == null || !bankName.toLowerCase().contains(searchBank.toLowerCase())) {
                            return false;
                        }
                    }
                    
                    return true;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Cancel withdraw request and refund money to wallet
     */
    @Transactional
    public void cancelWithdrawRequest(Long requestId) {
        // Get current user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();
        
        // Find withdraw request
        WithdrawRequest request = withdrawRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu rút tiền"));
        
        // Check if user owns this request
        Shop shop = shopRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Bạn chưa có gian hàng"));
        
        if (!request.getShopId().equals(shop.getId())) {
            throw new RuntimeException("Bạn không có quyền hủy yêu cầu này");
        }
        
        // Check if request can be cancelled (only PENDING status)
        if (request.getStatus() != WithdrawRequest.Status.PENDING) {
            throw new RuntimeException("Chỉ có thể hủy yêu cầu đang chờ duyệt");
        }
        
        // Get user's wallet
        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví của bạn"));
        
        // Refund money to wallet
        BigDecimal refundAmount = request.getAmount();
        wallet.setBalance(wallet.getBalance().add(refundAmount));
        walletRepository.save(wallet);
        
        // Update original wallet history from PENDING to CANCELED
        walletHistoryRepository.findByWalletIdAndReferenceIdAndTypeAndStatus(
                wallet.getId(), 
                request.getId().toString(), 
                WalletHistory.Type.WITHDRAW, 
                WalletHistory.Status.PENDING
        ).ifPresent(walletHistory -> {
            walletHistory.setStatus(WalletHistory.Status.CANCELED);
            walletHistory.setDescription("Yêu cầu rút tiền #" + request.getId() + " đã bị hủy - Tiền đã được hoàn trả");
            walletHistory.setUpdatedAt(java.time.Instant.now());
            walletHistoryRepository.save(walletHistory);
        });
        
        // Create wallet history for refund
        WalletHistory refundHistory = WalletHistory.builder()
                .walletId(wallet.getId())
                .amount(refundAmount)
                .type(WalletHistory.Type.REFUND)
                .status(WalletHistory.Status.SUCCESS)
                .description("Hoàn tiền từ hủy yêu cầu rút tiền #" + request.getId())
                .build();
        walletHistoryRepository.save(refundHistory);
        
        // Update request status to CANCELLED
        request.setStatus(WithdrawRequest.Status.CANCELLED);
        withdrawRequestRepository.save(request);
        
        log.info("Withdraw request {} cancelled and {} VND refunded to user {}", 
                requestId, refundAmount, user.getId());
    }
}
