package com.badat.study1.admin.service;

import com.badat.study1.admin.util.AdminBeanUtil;
import com.badat.study1.model.Complaint;
import com.badat.study1.repository.ComplaintRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminDisputeService {
    private final ComplaintRepository complaintRepository;

    public AdminDisputeService(ComplaintRepository complaintRepository) {
        this.complaintRepository = complaintRepository;
    }

    public List<Complaint> listAll() {
        return complaintRepository.findAll();
    }

    @Transactional
    public void resolve(Long complaintId, String resolution, String note) {
        var c = complaintRepository.findById(complaintId).orElseThrow();
        // ❌ trước đây: setStatus(String)
        // ✅ parse enum để đúng kiểu
        Complaint.Status st = AdminBeanUtil.parseEnum(Complaint.Status.class, resolution);
        c.setStatus(st);

        // nếu entity có field adminNote thì set, không thì bỏ qua
        AdminBeanUtil.setString(c, "adminNote", note);
        complaintRepository.save(c);
    }
}
