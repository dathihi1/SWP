package com.badat.study1.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithdrawRequestDto {
    
    private BigDecimal amount;
    private String bankAccountNumber;
    private String bankAccountName;
    private String bankName;
    
    private String note;
}
