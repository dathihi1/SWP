package com.badat.study1.admin.service;

import com.badat.study1.admin.util.AdminBeanUtil;
import com.badat.study1.model.WithdrawRequest;
import com.badat.study1.repository.WithdrawRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminWithdrawService {
    private final WithdrawRequestRepository repo;

    public AdminWithdrawService(WithdrawRequestRepository repo) {
        this.repo = repo;
    }

    public List<WithdrawRequest> listAll() {
        return repo.findAll();
    }

    @Transactional
    public void setStatus(Long id, String status, String note) {
        var w = repo.findById(id).orElseThrow();
        // ❌ trước: setStatus(String)
        // ✅ parse đúng enum
        WithdrawRequest.Status st = AdminBeanUtil.parseEnum(WithdrawRequest.Status.class, status);
        w.setStatus(st);
        AdminBeanUtil.setString(w, "adminNote", note);
        repo.save(w);
    }
}
