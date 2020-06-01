package com.github.springboot.service;

import java.util.List;
import java.util.Optional;

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
     * @return personDto
     */
    Optional<PersonDto> findById(String id);

    /**
     * Return list of active companies.
     * @param pageSize page size
     * @return list of users
     */
    List<PersonDto> findAll(Integer pageSize);

    /**
     * Return list of active companies by user
     * @param name user
     * @param pageSize page size
     * @return list of companies
     */
    List<PersonDto> findPeopleByCreatedUser(String name, Integer pageSize);

    /**
     * Delete a user by id.
     * @param id id
     */
    void deleteById(String id);
}
