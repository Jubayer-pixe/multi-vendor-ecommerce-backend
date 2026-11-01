package com.abc.SpringBootSecqurityEx.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class WishlistItemDTO {
    private Long id;
    private Long wishlistId;
    private Long productId;
    private Integer quantity;
    private LocalDateTime createdAt;
}
