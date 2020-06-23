package com.github.todo;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import org.apache.http.HttpHeaders;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

@QuarkusTest
@QuarkusTestResource(EmbeddedMongoQuarkusTestResource.class)
@QuarkusTestResource(WireMockQuarkusTestResource.class)
public class TodoResourceTest {
    @Inject
    AppLifecycleBean appLifecycleBean;

    @Inject
    JacksonCustomizer jacksonCustomizer;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "quarkus.http.test-port")
    Integer assignedPort;

    static TokenUtils tokenUtils;

    @BeforeAll
    public static void init() {
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
        tokenUtils = TokenUtils.getInstance();
    }

    @BeforeEach
    public void setup() {
        jacksonCustomizer.customize(objectMapper);
        Todo.deleteAll();
        appLifecycleBean.onStart(Mockito.mock(StartupEvent.class));
    }

    @Test
    @DisplayName("Test - When Calling DELETE - /api/todos/{id} with invalid permission should response forbidden - 403")
    public void testDeleteTodo() throws Exception {
        Todo todo = new Todo();
        todo.name = "Test";
        todo.personId = "admin";
        todo.persist();

        String authorization = tokenUtils.generateTokenString(new TokenUtils.AuthorizationDto("test", new String[] {"todo:create"}));

        given()
                .when()
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .delete("/api/todos/{id}", todo.id.toHexString())
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("Test - When Calling POST - /api/todos with admin user should create resource - 201")
    public void testCreateTodo() throws Exception {
        TodoDto todoDto = new TodoDto();
        todoDto.setName("new Todo");
        todoDto.setPersonId("admin");
        todoDto.setCategory(Category.OTHER);

        String authorization = tokenUtils.generateTokenString(new TokenUtils.AuthorizationDto("test", new String[] {"todo:create"}));

        given()
                .when()
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .body(todoDto)
                .contentType(ContentType.JSON)
                .post("/api/todos")
                .then()
                .statusCode(201)
                .header(HttpHeaders.LOCATION, containsString("/api/todos/"))
                .body("createdDate", is(notNullValue()))
                .body("personId", is("admin"))
                .body("name", is("new Todo"));
    }

    @Test
    @DisplayName("Test - When Calling DELETE - /api/todos/{id} should response 204 - No Content")
    public void testDeleteTodoWithoutRoleShouldResponseForbidden() throws Exception {
        Todo todo = new Todo();
        todo.name = "Test";
        todo.personId = "test";
        todo.persist();

        String authorization = tokenUtils.generateTokenString(new TokenUtils.AuthorizationDto("test", new String[] {"todo:delete"}));

        given()
                .when()
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .delete("/api/todos/{id}", todo.id.toHexString())
                .then()
                .statusCode(204);
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/todos should response all todos - 200 - OK")
    public void testGetAllTodos() throws Exception {
        String authorization = tokenUtils.generateTokenString(new TokenUtils.AuthorizationDto("admin", new String[] {"admin"}));

        given()
            .when()
            .header(HttpHeaders.AUTHORIZATION, authorization)
            .get("/api/todos")
            .then()
            .statusCode(200)
            .body("$.size", equalTo(3))
            .body("name", hasItems("Learn Quarkus", "Learn Kotlin", "Learn Hurling"))
            .body("category", hasItems("LEARN", "LEARN", "HOBBY"))
            .body("personName", hasItems("Test", "Test", "Test"));
    }

    @Test
    @DisplayName("Test - When Calling GET - /api/todos/getTotalCategory should response all todos - 200 - OK")
    public void testGetTotalCategory() throws Exception {
        String authorization = tokenUtils.generateTokenString(new TokenUtils.AuthorizationDto("test", new String[] {"test"}));

        given()
            .when()
            .header(HttpHeaders.AUTHORIZATION, authorization)
            .get("/api/todos/getTotalCategory?personId=default@admin.com&done=false")
            .then()
            .statusCode(200)
            .body("LEARN.name", hasItems("Learn Quarkus", "Learn Kotlin"))
            .body("HOBBY.name", hasItems("Learn Hurling"));

        given()
                .when()
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .get("/api/todos/getTotalCategory?categoryName=LEARN")
                .then()
                .statusCode(200)
                .body("LEARN.name", hasItems("Learn Quarkus", "Learn Kotlin"))
                .body("$", not(hasKey("HOBBY")));

        given()
                .when()
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .get("/api/todos/getTotalCategory?categoryName=HO")
                .then()
                .statusCode(200)
                .body("HOBBY.name", hasItems("Learn Hurling"))
                .body("$", not(hasKey("LEARN")));
    }

}
