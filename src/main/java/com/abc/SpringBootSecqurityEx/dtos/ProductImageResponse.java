package com.abc.SpringBootSecqurityEx.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProductImageResponse {
    private Long id;
    private Long productId;
    private String url;
    private String altText;
    private Boolean primaryImage;
    private String contentType;
    private Long sizeBytes;
}
