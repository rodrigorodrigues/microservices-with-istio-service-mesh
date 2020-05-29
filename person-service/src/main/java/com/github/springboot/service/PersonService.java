package com.github.springboot.service;

import java.util.Optional;
import java.util.stream.Stream;

import com.github.springboot.dto.PersonDto;

/**
 * Service for Person.
 */
public interface PersonService {
    /**
     * Save a user.
     * @param personDto
     * @return personDto
     */
    Optional<PersonDto> save(PersonDto personDto);

    /**
     * Return a Person by id.
     * @param id id
     * @return companyDto
     */
    Optional<PersonDto> findById(String id);

    /**
     * Return list of active companies.
     * @param pageSize page size
     * @return list of users
     */
    Stream<PersonDto> findAll(Integer pageSize);

    /**
     * Return list of active companies by user
     * @param name user
     * @param pageSize page size
     * @return list of companies
     */
    Stream<PersonDto> findPeopleByCreatedUser(String name, Integer pageSize);

    /**
     * Delete a user by id.
     * @param id id
     */
    void deleteById(String id);
}
