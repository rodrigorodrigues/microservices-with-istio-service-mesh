package com.github.microservices.service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.github.microservices.dto.PersonDto;
import com.github.microservices.model.Person;
import com.github.microservices.repository.PersonRepository;
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
        when(personRepository.save(any())).thenReturn(person);

        Optional<PersonDto> save = personService.save(any());

        assertThat(save.isPresent()).isTrue();
    }

    @Test
    public void whenCallFindByIdShouldFindPerson() {
        Optional<Person> person = Optional.of(new Person("Test", "me"));
        when(personRepository.findById(anyString())).thenReturn(person);

        Optional<PersonDto> personDto = personService.findById("123");

        assertThat(personDto.isPresent()).isTrue();
        assertThat(personDto.get().getId()).isNotEmpty();
        assertThat(personDto.get().getName()).isEqualTo("Test");
        assertThat(personDto.get().getCreatedByUser()).isEqualTo("me");
    }

    @Test
    public void whenCallFindAllActiveCompaniesShouldReturnListOfPeople() {
        when(personRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(Arrays.asList(new Person(), new Person(), new Person())));

        List<PersonDto> people = personService.findAll(10);

        assertThat(people.size()).isEqualTo(3);
    }

    @Test
    public void whenCallFindCompaniesByUserShouldReturnListOfPeople() {
        when(personRepository.findPeopleByCreatedUser(anyString(), any(Pageable.class))).thenReturn(Stream.of(new Person(), new Person()));

        List<PersonDto> people = personService.findPeopleByCreatedUser("me", 10);

        assertThat(people.size()).isEqualTo(2);
    }

    @Test
    public void whenCallDeleteByIdShouldDeletePeople() {
        personService.deleteById("123");

        verify(personRepository).deleteById("123");
    }

}
