package com.abc.SpringBootSecqurityEx.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CustomerDTO {
    private Long id;
    private String userName;
    private String fullName;
    private String phone;
    private LocalDate dateOfBirth;
    private Integer loyaltyPoints;
    private LocalDateTime createdAt;
    private List<Long> addressIds;
    private List<Long> wishlistIds;
}
