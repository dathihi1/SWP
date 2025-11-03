package com.badat.study1.service;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.badat.study1.model.User;

public interface SellerService {
    String submitSellerRegistration(String ownerName,
                                    String identity,
                                    String bankAccountName,
                                    String agree,
                                    User currentUser,
                                    RedirectAttributes redirectAttributes);

    String getSellerRegisterPage(User currentUser,
                                 org.springframework.ui.Model model,
                                 RedirectAttributes redirectAttributes);
}


