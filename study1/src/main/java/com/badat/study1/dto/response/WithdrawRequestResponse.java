package com.badat.study1.dto.response;

import com.badat.study1.model.WithdrawRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithdrawRequestResponse {
    
    private Long id;
    private Long shopId;
    private String shopName;
    private BigDecimal amount;
    private WithdrawRequest.Status status;
    private String bankAccountNumber;
    private String bankAccountName;
    private String bankName;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static WithdrawRequestResponse fromEntity(WithdrawRequest entity) {
        return WithdrawRequestResponse.builder()
                .id(entity.getId())
                .shopId(entity.getShopId())
                .amount(entity.getAmount())
                .status(entity.getStatus())
                .bankAccountNumber(entity.getBankAccountNumber())
                .bankAccountName(entity.getBankAccountName())
                .bankName(entity.getBankName())
                .note(entity.getNote())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
