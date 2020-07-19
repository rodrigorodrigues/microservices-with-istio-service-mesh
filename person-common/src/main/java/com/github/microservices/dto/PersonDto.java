package com.github.microservices.dto;

import java.time.Instant;

import javax.validation.constraints.NotBlank;

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
    private Instant lastModifiedDate;
    private String hostName;
}
