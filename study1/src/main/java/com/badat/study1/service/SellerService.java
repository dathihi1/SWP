package com.badat.study1.service;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;
import com.badat.study1.model.User;

public interface SellerService {
    String submitSellerRegistration(String ownerName,
                                    String shortDescription,
                                    MultipartFile cccdFront,
                                    MultipartFile cccdBack,
                                    String agree,
                                    User currentUser,
                                    RedirectAttributes redirectAttributes);

    String getSellerRegisterPage(User currentUser,
                                 org.springframework.ui.Model model,
                                 RedirectAttributes redirectAttributes);
}


