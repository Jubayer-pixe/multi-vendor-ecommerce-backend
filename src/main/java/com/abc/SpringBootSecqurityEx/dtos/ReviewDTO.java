package com.abc.SpringBootSecqurityEx.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ReviewDTO {
    private Long id;
    private Long productId;
    private Long customerId;
    private Integer rating;
    private String title;
    private String comment;
    private LocalDateTime createdAt;
}
