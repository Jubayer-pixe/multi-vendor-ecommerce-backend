package com.abc.SpringBootSecqurityEx.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VendorBankAccountDTO {
    private Long id;
    private Long vendorId;
    private String bankName;
    private String accountHolderName;
    private String accountNumber;
    private String ifscCode;
    private Boolean primaryAccount;
}
