package com.grupo.spent.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegisterResponseDto {
    private String email;
    private String username;
    private String name;
}
