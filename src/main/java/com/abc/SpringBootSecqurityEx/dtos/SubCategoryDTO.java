package com.abc.SpringBootSecqurityEx.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SubCategoryDTO {
    private Long id;
    private Long categoryId;
    private String name;
    private String slug;
}
