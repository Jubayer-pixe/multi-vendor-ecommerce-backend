package com.abc.SpringBootSecqurityEx.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProductAttributeValueDTO {
    private Long id;
    private Long productVariantId;
    private Long attributeId;
    private String value;
}
