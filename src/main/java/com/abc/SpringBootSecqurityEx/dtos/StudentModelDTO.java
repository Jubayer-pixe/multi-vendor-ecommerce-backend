package com.abc.SpringBootSecqurityEx.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StudentModelDTO {
    private Integer studentId;
    private String studentName;
    private String address;
    private Float semisterCost;
    private Integer semisterNo;
}
