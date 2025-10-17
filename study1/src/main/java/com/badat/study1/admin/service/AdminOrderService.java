package com.badat.study1.admin.service;

import com.badat.study1.model.Transaction;
import com.badat.study1.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Phù hợp với AdminOrderController hiện có:
 * - listAll(): trả về List<Transaction>
 * - updateStatus(Long id, String status): nhận status dạng String
 *
 * Không bắt buộc AuditLogService (tiêm optional), để tránh lỗi "No beans of 'AuditLogService'".
 * Nếu sau này bạn tạo AuditLogService, chỉ cần khai báo bean là sẽ tự log được.
 */
@Service
@Transactional
public class AdminOrderService {

    private final TransactionRepository transactionRepository;

    // OPTIONAL: audit log, không bắt buộc có bean
    private final AuditLogService auditLogService;

    public AdminOrderService(
            TransactionRepository transactionRepository,
            @org.springframework.beans.factory.annotation.Autowired(required = false) AuditLogService auditLogService
    ) {
        this.transactionRepository = transactionRepository;
        this.auditLogService = auditLogService;
    }

    /** Dùng cho GET /admin/orders trong AdminOrderController */
    public List<Transaction> listAll() {
        return transactionRepository.findAll();
    }

    /** Dùng cho POST /admin/orders/{id}/status trong AdminOrderController */
    public void updateStatus(Long id, String statusText) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + id));

        // Parse string -> enum một cách an toàn (không ném exception lung tung lên UI)
        Transaction.Status newStatus;
        try {
            newStatus = Transaction.Status.valueOf(statusText.toUpperCase().trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid status: " + statusText);
        }

        Transaction.Status old = tx.getStatus();
        tx.setStatus(newStatus);
        transactionRepository.save(tx);

        // Ghi audit (nếu có service); không bắt buộc – tránh lỗi "No beans of 'AuditLogService'"
        if (auditLogService != null) {
            String details = String.format(
                    "{\"targetType\":\"Transaction\",\"targetId\":%d,\"from\":\"%s\",\"to\":\"%s\"}",
                    id, old, newStatus
            );
            auditLogService.log(null, "ORDER_STATUS_UPDATE", details, null);
        }
    }
}
