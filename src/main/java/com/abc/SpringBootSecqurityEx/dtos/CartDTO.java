package com.abc.SpringBootSecqurityEx.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CartDTO {
    private Long id;
    private Long customerId;
    private String status;
    private LocalDateTime updatedAt;
    private List<Long> itemIds;
}
