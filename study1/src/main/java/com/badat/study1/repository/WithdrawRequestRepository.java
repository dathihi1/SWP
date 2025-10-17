package com.badat.study1.repository;

import com.badat.study1.model.WithdrawRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WithdrawRequestRepository extends JpaRepository<WithdrawRequest, Long> {
    List<WithdrawRequest> findByShopId(Long shopId);
    List<WithdrawRequest> findByShopIdAndIsDeleteFalse(Long shopId);
    List<WithdrawRequest> findByStatus(WithdrawRequest.Status status);
    List<WithdrawRequest> findByShopIdOrderByCreatedAtDesc(Long shopId);
    List<WithdrawRequest> findByStatusOrderByCreatedAtDesc(WithdrawRequest.Status status);
}
