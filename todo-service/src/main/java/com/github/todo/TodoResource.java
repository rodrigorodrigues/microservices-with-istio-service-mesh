package com.github.todo;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.eureka.client.EurekaClient;
import io.quarkus.eureka.client.loadBalancer.LoadBalanced;
import io.quarkus.eureka.client.loadBalancer.LoadBalancerType;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.http.HttpHeaders;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Metered;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.mapstruct.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/api/todos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
@RegisterForReflection
public class TodoResource {
    private static final Logger log = LoggerFactory.getLogger(TodoResource.class);

    @Inject
    TodoMapper todoMapper;

    @Inject
    @LoadBalanced(type = LoadBalancerType.ROUND_ROBIN)
    EurekaClient eurekaClient;

    @Inject
    JsonWebToken jwt;

    @Inject
    ObjectMapper objectMapper;

    @GET
    @Timed(name = "getAllTodosTimed",
            description = "Monitor the time getAllTodos method takes",
            unit = MetricUnits.MILLISECONDS,
            absolute = true)
    @Metered(name = "getAllTodosMetered",
            unit = MetricUnits.MILLISECONDS,
            description = "Monitor the rate events occurred",
            absolute = true)
    @Counted(
            name = "getAllTodosCounted",
            absolute = true,
            displayName = "getAllTodos",
            description = "Monitor how many times getAllTodos method was called")
    @RolesAllowed({"todo:read", "admin"})
    @Fallback(fallbackMethod = "fallback")
    public Response getAllTodos(@QueryParam("pageSize") @DefaultValue("10") Integer pageSize,
            @Context SecurityContext ctx) {
        List<Todo> todos;
        if (ctx.isUserInRole("admin")) {
            todos = Todo.findAll(pageSize);
        } else {
            todos = Todo.findTodosByPersonId(pageSize, ctx.getUserPrincipal().getName());
        }
        List<TodoDto> todosDto = todoMapper.toResource(todos);
        todosDto.forEach(t -> t.setPersonName(getPersonNameByEureka(t.getPersonId())));
        return Response.ok(todosDto).build();
    }

    public Response fallback(Integer pageSize, SecurityContext ctx) {
        TodoDto todoDto = new TodoDto();
        todoDto.setCreatedDate(null);
        todoDto.setName(String.format("Some error occurred! - please try later - %s", ctx.getUserPrincipal().getName()));
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity(Collections.singletonList(todoDto))
                .build();
    }

    private String getPersonNameByEureka(String personId) {
        try {
            String json = eurekaClient.app("person-service")
                    .path("/api/people/" + personId)
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .header(HttpHeaders.AUTHORIZATION, jwt.getRawToken())
                    .get()
                    .readEntity(String.class);
            log.info("EurekaClient:json: {}", json);
            return objectMapper.readValue(json, PersonDto.class).name;
        } catch (Exception e) {
            log.warn("Error on method getPersonNameByEureka", e);
            return "mocked-name";
        }
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"todo:read", "admin"})
    public Response getById(@PathParam("id") String id, @Context SecurityContext ctx) {
        return getById(id)
                .map(t -> Response.ok(todoMapper.toResource(t)).build())
                .orElseThrow(NotFoundException::new);
    }

    @POST
    @RolesAllowed({"todo:create", "admin"})
    public Response create(@Valid TodoDto todoDto) {
        Todo todo = todoMapper.toModel(todoDto);
        todo.persist();
        return Response.created(URI.create(String.format("/api/todos/%s", todo.id)))
                                .entity(todoMapper.toResource((todo)))
                                .build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"todo:update", "admin"})
    public Response update(@Valid TodoDto todoDto, @PathParam("id") String id) {
        return getById(id)
                .map(t -> {
                    t.name = todoDto.getName();
                    t.personId = todoDto.getPersonId();
                    if (todoDto.getDone() != null) {
                        t.done = todoDto.getDone();
                    }
                    t.update();
                    return Response.ok(t).build();
                })
                .orElseThrow(NotFoundException::new);
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"todo:delete", "admin"})
    public Response delete(@PathParam("id") String id) {
        return getById(id)
                .map(t -> {
                    t.delete();
                    return Response.noContent().build();
                })
                .orElseThrow(NotFoundException::new);
    }

    private Optional<Todo> getById(String id) {
        return Todo.findByIdOptional(new ObjectId(id));
    }

    @Mapper(componentModel = "cdi")
    interface TodoMapper {
        TodoDto toResource(Todo todo);
        default List<TodoDto> toResource(List<Todo> todos) {
            return todos.stream()
                    .map(t -> new TodoDto(t.name, t.personId, null, t.done, t.createdDate, t.plannedEndDate, t.category))
                    .collect(Collectors.toList());
        }
        Todo toModel(TodoDto todoDto);
    }
}
