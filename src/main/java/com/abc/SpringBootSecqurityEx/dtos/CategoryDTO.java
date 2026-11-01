package com.abc.SpringBootSecqurityEx.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
public class CategoryDTO {
    private Long id;
    @NotBlank
    @Size(max = 255)
    private String name;
    @NotBlank
    @Size(max = 255)
    private String slug;
    @Size(max = 1000)
    private String imageUrl;
    private Boolean active;
}
