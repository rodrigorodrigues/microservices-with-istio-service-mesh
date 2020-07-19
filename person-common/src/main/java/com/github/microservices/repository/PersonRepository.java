package com.github.microservices.repository;

import java.util.stream.Stream;

import com.github.microservices.model.Person;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Person Object using MongoDB.
 * Name convention are binding using Spring Data MongoDB - https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/#repositories.query-methods.query-creation
 */
@Repository
public interface PersonRepository extends PagingAndSortingRepository<Person, String> {
    @Query("select p from Person p where p.createdByUser = ?1")
    Stream<Person> findPeopleByCreatedUser(String user, final Pageable page);
}
