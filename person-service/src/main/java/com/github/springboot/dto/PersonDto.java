package com.github.springboot.dto;

import java.time.Instant;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PersonDto {
    private String id;
    @NotBlank
    private String name;
    private String createdByUser;
    @NotNull
    private Instant lastModifiedDate;
}
