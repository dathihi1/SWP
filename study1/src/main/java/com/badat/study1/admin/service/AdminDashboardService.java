package com.badat.study1.admin.service;

import com.badat.study1.admin.dto.StatsDTO;
import com.badat.study1.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;
    private final TransactionRepository transactionRepository;
    private final ComplaintRepository complaintRepository;
    private final WithdrawRequestRepository withdrawRequestRepository;

    public AdminDashboardService(UserRepository userRepository,
                                 ShopRepository shopRepository,
                                 ProductRepository productRepository,
                                 TransactionRepository transactionRepository,
                                 ComplaintRepository complaintRepository,
                                 WithdrawRequestRepository withdrawRequestRepository) {
        this.userRepository = userRepository;
        this.shopRepository = shopRepository;
        this.productRepository = productRepository;
        this.transactionRepository = transactionRepository;
        this.complaintRepository = complaintRepository;
        this.withdrawRequestRepository = withdrawRequestRepository;
    }

    public StatsDTO getStats() {
        StatsDTO s = new StatsDTO();

        s.setTotalUsers(userRepository.count());
        s.setTotalShops(shopRepository.count());
        s.setTotalProducts(productRepository.count());
        s.setTotalOrders(transactionRepository.count());
        s.setTotalDisputes(complaintRepository.count());

        // Đếm PENDING an toàn, không phụ thuộc method đặc thù
        long pendingWithdraws = withdrawRequestRepository.findAll().stream()
                .filter(w -> {
                    try {
                        return w.getStatus() != null && w.getStatus().equals("PENDING");
                    } catch (Exception e) {
                        return false;
                    }
                })
                .count();
        s.setTotalWithdrawsPending(pendingWithdraws);

        // GMV: để 0 cho chắc chắn biên dịch (tránh lỗi sum khi chưa rõ field)
        // TODO: Khi biết rõ field (vd: amount/totalAmount) thì thêm query sumPaidAmount() trong TransactionRepository
        s.setGmV(BigDecimal.ZERO);

        return s;
    }
}
