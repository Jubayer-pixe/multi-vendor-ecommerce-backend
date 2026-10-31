package com.abc.SpringBootSecqurityEx.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ProductVariantDTO {
    private Long id;
    private Long productId;
    private String name;
    private String sku;
    private BigDecimal price;
    private Integer stockQuantity;
    private List<Long> attributeValueIds;
}
