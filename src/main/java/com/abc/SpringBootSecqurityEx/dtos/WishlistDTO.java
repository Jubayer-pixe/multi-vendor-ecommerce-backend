package com.abc.SpringBootSecqurityEx.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class WishlistDTO {
    private Long id;
    private Long customerId;
    private String name;
    private LocalDateTime createdAt;
    private List<Long> itemIds;
}
