package com.github.springboot.repository;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springboot.model.Person;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@ExtendWith(SpringExtension.class)
@DataMongoTest(properties = {"configuration.initialLoad=false", "logging.level.com.github.springboot=debug"})
@Import({ObjectMapper.class})
public class PersonRepositoryTest {
    @Autowired
    PersonRepository personRepository;

    @BeforeEach
    public void setup() {
        personRepository.save(Person.builder().name("Test")
                .createdByUser("me")
                .build());

        personRepository.save(Person.builder().name("Test not active")
                .createdByUser("me")
                .build());

        personRepository.save(Person.builder().name("Test 2")
                .createdByUser("another_user")
                .build());
    }

    @Test
    public void findAllStream() {
        assertThat(personRepository.findAll()).hasSize(3);
    }

    @Test
    public void testFindAllPeople() {
        List<Person> people = personRepository.findAll(PageRequest.of(0, 10))
            .stream()
            .collect(Collectors.toList());

        assertThat(people.size()).isEqualTo(3);

        people = personRepository.findAll(PageRequest.of(0, 1))
            .stream()
            .collect(Collectors.toList());

        assertThat(people.size()).isEqualTo(1);
        assertThat(Stream.of(people.toArray(new Person[] {})).map(Person::getName))
            .containsExactlyInAnyOrder("Test");
    }

    @Test
    public void testFindByUser() {
        List<Person> people = personRepository.findPeopleByCreatedUser("another_user", PageRequest.of(0, 1))
            .collect(Collectors.toList());

        assertThat(people.size()).isEqualTo(1);
        assertThat(people.get(0).getName()).isEqualTo("Test 2");
    }

    @AfterEach
    public void tearDown() {
        personRepository.deleteAll();
    }
}
