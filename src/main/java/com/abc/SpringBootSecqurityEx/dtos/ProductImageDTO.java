package com.abc.SpringBootSecqurityEx.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProductImageDTO {
    private Long id;
    private Long productId;
    private String imageUrl;
    private String altText;
    private Boolean primaryImage;
}
