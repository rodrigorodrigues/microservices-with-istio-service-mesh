package com.github.springboot.repository;

import java.util.stream.Stream;

import com.github.springboot.model.Person;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository for Person Object using MongoDB.
 * Name convention are binding using Spring Data MongoDB - https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/#repositories.query-methods.query-creation
 */
@Repository
public interface PersonRepository extends MongoRepository<Person, String> {
    @Query("{'createdByUser': ?0}")
    Stream<Person> findPeopleByCreatedUser(String user, final Pageable page);
}
