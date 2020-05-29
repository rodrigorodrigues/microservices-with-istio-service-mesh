package com.github.springboot.service;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import com.github.springboot.dto.PersonDto;
import com.github.springboot.model.Person;
import com.github.springboot.repository.PersonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PersonServiceImplTest {

    PersonService personService;

    @Mock
    PersonRepository personRepository;

    PersonServiceImpl.PersonMapper personMapper = new PersonServiceImpl$PersonMapperImpl();

    @BeforeEach
    public void setup() {
        personService = new PersonServiceImpl(personRepository, personMapper);
    }

    @Test
    public void whenCallSaveShouldSavePerson() {
        Person person = new Person();
        when(personRepository.save(any())).thenReturn(Optional.of(person));

        PersonDto personDto = new PersonDto();
        Optional<PersonDto> save = personService.save(personDto);

        assertThat(save.isPresent()).isTrue();
    }

    @Test
    public void whenCallFindByIdShouldFindPerson() {
        Optional<Person> person = Optional.of(Person.builder().id("1").name("Test").build());
        when(personRepository.findById(anyString())).thenReturn(person);

        Optional<PersonDto> personDto = personService.findById("123");

        assertThat(personDto.isPresent()).isTrue();
        assertThat(personDto.get().getId()).isEqualTo("1");
        assertThat(personDto.get().getName()).isEqualTo("Test");
    }

    @Test
    public void whenCallFindAllActiveCompaniesShouldReturnListOfPeople() {
        when(personRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(Arrays.asList(new Person(), new Person(), new Person())));

        Stream<PersonDto> people = personService.findAll(10);

        assertThat(people.count()).isEqualTo(3);
    }

    @Test
    public void whenCallFindCompaniesByUserShouldReturnListOfPeople() {
        when(personRepository.findPeopleByCreatedUser(anyString(), any(Pageable.class))).thenReturn(Stream.of(new Person(), new Person()));

        Stream<PersonDto> people = personService.findPeopleByCreatedUser("me", 10);

        assertThat(people.count()).isEqualTo(2);
    }

    @Test
    public void whenCallDeleteByIdShouldDeletePeople() {
        personService.deleteById("123");

        verify(personRepository).deleteById("123");
    }

}
