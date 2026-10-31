package com.abc.SpringBootSecqurityEx.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VendorDocumentDTO {
    private Long id;
    private Long vendorId;
    private String documentType;
    private String documentNumber;
    private String fileUrl;
    private String status;
}
