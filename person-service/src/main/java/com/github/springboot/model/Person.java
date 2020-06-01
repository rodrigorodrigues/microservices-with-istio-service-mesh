package com.github.springboot.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@NoArgsConstructor
@RequiredArgsConstructor
@Entity
@Table(name = "people")
public class Person implements Serializable {
    @Id
    private String id = UUID.randomUUID().toString();

    @NotEmpty
    @NonNull
    private String name;

    @NotEmpty
    @NonNull
    private String createdByUser;

    @NotNull
    @NonNull
    private Instant lastModifiedDate = Instant.now();
}
