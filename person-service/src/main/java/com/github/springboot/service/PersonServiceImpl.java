package com.github.springboot.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.springboot.dto.PersonDto;
import com.github.springboot.model.Person;
import com.github.springboot.repository.PersonRepository;
import lombok.AllArgsConstructor;
import org.mapstruct.Mapper;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Transactional
public class PersonServiceImpl implements PersonService {
    private final PersonRepository personRepository;

    private final PersonMapper personMapper;

    public Optional<PersonDto> save(PersonDto personDto) {
        Person person = personMapper.dtoToEntity(personDto);
        return Optional.of(personMapper.entityToDto(personRepository.save(person)));
    }

    @Override
    public Optional<PersonDto> findById(String id) {
        return personRepository.findById(id)
			.map(personMapper::entityToDto);
    }

    @Override
    public List<PersonDto> findAll(Integer pageSize) {
        return personRepository.findAll(PageRequest.of(0, pageSize))
			.stream()
			.map(personMapper::entityToDto)
			.collect(Collectors.toList());
    }

    @Override
    public List<PersonDto> findPeopleByCreatedUser(String name, Integer pageSize) {
        return personRepository.findPeopleByCreatedUser(name, PageRequest.of(0, pageSize))
			.map(personMapper::entityToDto)
			.collect(Collectors.toList());
    }

    @Override
    public void deleteById(String id) {
        personRepository.deleteById(id);
    }

	@Mapper(componentModel = "spring")
	interface PersonMapper {
		Person dtoToEntity(PersonDto personDto);

		PersonDto entityToDto(Person person);
	}
}
