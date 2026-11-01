package com.abc.SpringBootSecqurityEx.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DashboardRecentUserDTO {
    private String username;
    private String email;
    private List<String> roles;
    private Boolean enabled;
    private Instant dateCreated;
}
