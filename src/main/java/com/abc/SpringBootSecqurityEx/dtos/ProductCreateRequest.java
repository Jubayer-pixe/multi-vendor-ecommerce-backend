package com.abc.SpringBootSecqurityEx.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Client-editable catalog fields for canonical product creation; ownership remains server-assigned. */
@Getter
@Setter
@NoArgsConstructor
public class ProductCreateRequest {
    @NotBlank
    @Size(max = 150)
    private String name;

    @NotBlank
    @Size(max = 180)
    private String slug;

    @Size(max = 2000)
    private String description;

    @NotBlank
    @Size(max = 100)
    private String sku;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal basePrice;

    @DecimalMin("0.00")
    private BigDecimal salePrice;

    private Long categoryId;
    @Size(max = 100)
    private String categoryName;
    private Long subCategoryId;
    private Long brandId;
    @Size(max = 100)
    private String brandName;
    @Min(0)
    private Integer stockQuantity;
    private Boolean active;
}
