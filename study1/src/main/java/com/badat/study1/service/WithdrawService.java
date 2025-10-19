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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawService {
    
    private final WithdrawRequestRepository withdrawRequestRepository;
    private final ShopRepository shopRepository;
    private final WalletRepository walletRepository;
    private final WalletHistoryRepository walletHistoryRepository;
    
    @Transactional
    public WithdrawRequestResponse createWithdrawRequest(WithdrawRequestDto requestDto) {
        // Get current user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();
        
        // Find user's shop
        Shop shop = shopRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Bạn chưa có gian hàng"));
        
        // Get user's wallet
        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví của bạn"));
        
        // Validate amount
        if (requestDto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Số tiền phải lớn hơn 0");
        }
        
        // Minimum withdraw amount (50,000 VND)
        BigDecimal minimumAmount = new BigDecimal("50000");
        if (requestDto.getAmount().compareTo(minimumAmount) < 0) {
            throw new RuntimeException("Số tiền rút tối thiểu là 50,000 VNĐ");
        }
        
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
        
        // Check for pending withdraw requests (prevent duplicate requests)
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
        
        // Hold the amount from wallet (subtract from available balance)
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
}
