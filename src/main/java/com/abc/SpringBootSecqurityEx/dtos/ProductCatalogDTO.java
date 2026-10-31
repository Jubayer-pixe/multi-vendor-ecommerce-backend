package com.abc.SpringBootSecqurityEx.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ProductCatalogDTO {
    private Long id;

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

    @Min(0)
    private Integer stockQuantity;
    private Boolean active;
    private Long categoryId;
    private String categoryName;
    private Long subCategoryId;
    private Long brandId;
    private String brandName;
    private Long vendorId;
    private LocalDateTime createdAt;
    private List<Long> imageIds;
    private List<Long> variantIds;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ProductImageResponse> images;
}
