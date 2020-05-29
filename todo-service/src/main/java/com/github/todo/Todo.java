package com.github.todo;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

import javax.validation.constraints.NotBlank;

import io.quarkus.mongodb.panache.MongoEntity;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.panache.common.Page;

@MongoEntity(collection = "todos")
public class Todo extends PanacheMongoEntity implements Serializable {
	@NotBlank
	public String name;
	@NotBlank
	public String personId;
	public boolean activated = true;
	public Instant lastModifiedDate = Instant.now();

	public static List<Todo> findActiveTodos(Integer pageSize) {
		return find("activated", true)
				.page(Page.ofSize(pageSize))
				.list();
	}

	public static List<Todo> findActiveTodosByPerson(Integer pageSize, String person) {
	    return find("activated = ?1 and personId = ?2", true, person)
            .page(Page.ofSize(pageSize))
            .list();
	}
}
