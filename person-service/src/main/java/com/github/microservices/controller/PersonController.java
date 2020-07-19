package com.github.microservices.controller;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.validation.Valid;

import com.github.microservices.dto.PersonDto;
import com.github.microservices.service.PersonService;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import springfox.documentation.annotations.ApiIgnore;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Rest API for people.
 */
@Slf4j
@RestController
@Api(value = "Methods for managing people")
@RequestMapping("/api/people")
@AllArgsConstructor
public class PersonController {
    private final PersonService personService;

    @ApiOperation(value = "Api for return list of people")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SCOPE_people:read', 'SCOPE_admin')")
    @HystrixCommand(fallbackMethod = "fallback")
    public ResponseEntity<List<PersonDto>> findAll(@ApiIgnore @AuthenticationPrincipal Authentication authentication,
        @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        log.debug("Hello({}) is authenticated? ({})", authentication.getName(), authentication.isAuthenticated());
        List<PersonDto> people;
        if (hasRoleAdmin(authentication)) {
            people = personService.findAll(pageSize);
        } else {
            people = personService.findPeopleByCreatedUser(authentication.getName(), pageSize);
        }
        return ResponseEntity.ok(people);
    }

    public ResponseEntity<List<PersonDto>> fallback(Authentication authentication, Integer pageSize) {
        PersonDto personDto = PersonDto.builder()
                .name(String.format("Some error occurred! - please try later - %s", authentication.getName())).build();
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Collections.singletonList(personDto));
    }

    @ApiOperation(value = "Api for return a person by id")
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SCOPE_people:read', 'SCOPE_todo:read', 'SCOPE_admin')")
    public ResponseEntity<PersonDto> findById(@ApiParam(required = true) @PathVariable String id,
                                    @ApiIgnore @AuthenticationPrincipal Authentication authentication) {
        log.debug("Hello({}) is authenticated? ({})", authentication.getName(), authentication.isAuthenticated());
        return personService.findById(id)
                .map(p -> {
                    if (hasRoleAdmin(authentication) || p.getCreatedByUser().equals(authentication.getName())) {
                        return ResponseEntity.ok(p);
                    } else {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, String
                                .format("User(%s) does not have access to this resource", authentication
                                        .getName()));
                    }
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @ApiOperation(value = "Api for creating a person")
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SCOPE_people:create', 'SCOPE_admin')")
    public ResponseEntity<PersonDto> create(@RequestBody @ApiParam(required = true) @Valid PersonDto personDto,
            @ApiIgnore @AuthenticationPrincipal Authentication authentication) {
        personDto.setCreatedByUser(authentication.getName());
        if (StringUtils.isBlank(personDto.getId())) {
            personDto.setId(UUID.randomUUID().toString());
        }
        return personService.save(personDto)
                .map(p -> ResponseEntity.created(URI.create(String.format("/api/people/%s", p.getId())))
                        .body(p))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @ApiOperation(value = "Api for updating a person")
    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SCOPE_people:update', 'SCOPE_admin')")
    public ResponseEntity<PersonDto> update(@RequestBody @ApiParam(required = true) @Valid PersonDto personDto,
                                  @PathVariable @ApiParam(required = true) String id,
                                  @ApiIgnore @AuthenticationPrincipal Authentication authentication) {
        personDto.setId(id);
        PersonDto personDtoUpdate = personService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        personDtoUpdate.setCreatedByUser(authentication.getName());
        personDtoUpdate.setName(personDto.getName());
        personDtoUpdate.setLastModifiedDate(Instant.now());
        return personService.save(personDtoUpdate)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @ApiOperation(value = "Api for deleting a person")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SCOPE_people:delete', 'SCOPE_admin')")
    public ResponseEntity delete(@PathVariable @ApiParam(required = true) String id,
                             @ApiIgnore @AuthenticationPrincipal Authentication authentication) {
        PersonDto personDto = personService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return Optional.of(personDto)
                .filter(p -> hasRoleAdmin(authentication) || p.getCreatedByUser().equals(authentication.getName()))
                .map(p -> {
                    personService.deleteById(p.getId());
                    return ResponseEntity.noContent().build();
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, String.format("User(%s) does not have access to delete this resource", authentication.getName())));
    }

    private boolean hasRoleAdmin(Authentication authentication) {
        return authentication.getAuthorities()
                .stream()
                .anyMatch(c -> "SCOPE_admin".equals(c.getAuthority()));
    }
}
