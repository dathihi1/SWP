package com.badat.study1.admin.service;

import com.badat.study1.model.AuditLog;
import com.badat.study1.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /* ===================== GHI LOG ===================== */

    /**
     * Ghi log cơ bản: ai (adminId) làm action gì, chi tiết, từ IP nào.
     * Không đổi schema: target (nếu có) bạn có thể tự chèn vào details.
     */
    @Transactional
    public AuditLog log(Long adminId, String action, String details, String ipAddress) {
        AuditLog log = AuditLog.builder()
                .userId(adminId)       // map sang cột user_id
                .action(action)
                .details(details)      // có thể chứa JSON/text
                .ipAddress(ipAddress)
                .build();
        return auditLogRepository.save(log);
    }

    /**
     * Tiện ích: cho phép ghi kèm "đối tượng" bị tác động (target) vào details dạng JSON-string,
     * mà không cần sửa entity (vì AuditLog hiện chưa có targetType/targetId).
     */
    @Transactional
    public AuditLog logWithTarget(Long adminId, String action,
                                  String targetType, Long targetId,
                                  String extraDetails, String ipAddress) {

        String details = String.format(
                "{\"targetType\":\"%s\",\"targetId\":%s,\"details\":%s}",
                safe(targetType),
                targetId == null ? "null" : targetId.toString(),
                toJsonString(extraDetails)
        );
        return log(adminId, action, details, ipAddress);
    }

    /* ===================== TRA CỨU ===================== */

    public Page<AuditLog> findAll(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }

    public List<AuditLog> findByUserId(Long userId) {
        return auditLogRepository.findByUserId(userId);
    }

    public List<AuditLog> findByAction(String action) {
        return auditLogRepository.findByAction(action);
    }

    /**
     * Nếu BaseEntity có cờ isDelete và repo đã có finder findByIsDeleteFalse(),
     * hàm này trả về toàn bộ log chưa xóa mềm (không phân trang).
     */
    public List<AuditLog> findActive() {
        return auditLogRepository.findByIsDeleteFalse();
    }

    /* ===================== QUẢN TRỊ (TÙY CHỌN) ===================== */

    /**
     * Soft delete 1 log (chỉ khi BaseEntity có field isDelete & setter).
     * Nếu BaseEntity không có setter cho isDelete thì bỏ method này đi.
     */
    @Transactional
    public void softDelete(Long id) {
        AuditLog log = auditLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("AuditLog not found: " + id));
        try {
            // giả định BaseEntity có setIsDelete(boolean)
            var method = log.getClass().getMethod("setIsDelete", boolean.class);
            method.invoke(log, true);
            auditLogRepository.save(log);
        } catch (ReflectiveOperationException e) {
            // Nếu BaseEntity không có setter isDelete -> bỏ soft delete hoặc đổi sang delete cứng:
            // auditLogRepository.delete(log);
            throw new IllegalStateException("AuditLog does not support soft-delete via isDelete field.", e);
        }
    }

    /* ===================== HELPERS ===================== */

    private String safe(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }

    private String toJsonString(String s) {
        if (s == null) return "null";
        return "\"" + safe(s) + "\"";
    }
}
