package com.github.springboot.model;

import java.io.Serializable;
import java.time.Instant;

import javax.validation.constraints.NotEmpty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "springboot_companies")
public class Person implements Serializable {
    @Id
    private String id;

    @NotEmpty
    private String name;

    private String createdByUser;

    private Instant lastModifiedDate = Instant.now();
}
