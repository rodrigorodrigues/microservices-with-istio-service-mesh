package com.github.todo;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey="person-api")
public interface PersonService {
    @GET
    @Path("/{personId}}")
    @Produces("application/json")
    PersonDto getById(@PathParam("personId") String personId, @HeaderParam("Authorization") String authorizationHeader);
}
