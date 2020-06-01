package com.github.todo;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import io.quarkus.mongodb.panache.MongoEntity;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import org.apache.commons.lang3.StringUtils;

@MongoEntity(collection = "todos")
public class Todo extends PanacheMongoEntity implements Serializable {
	@NotBlank
	public String name;
	@NotBlank
	public String personId;
	public boolean done;
	public Instant createdDate = Instant.now();
	public Instant plannedEndDate = Instant.now().plusSeconds(TimeUnit.DAYS.toSeconds(1));
	@NotNull
	public Category category;

	public static List<Todo> findAll(Integer pageSize) {
		return find("{}")
				.page(Page.ofSize(pageSize))
				.list();
	}

	public static List<Todo> findTodosByPersonId(Integer pageSize, String personId) {
	    return find("personId = ?1", personId)
            .page(Page.ofSize(pageSize))
            .list();
	}

	public static Map<Category, List<Todo>> findAllByCategory(Instant plannedEndDate, Boolean done) {
		StringBuilder queryString = new StringBuilder();
		int count = 0;
		if (done != null) {
			queryString.append("done = ?").append(++count);
		}
		if (plannedEndDate != null) {
			queryString.append("plannedEndDate <= ?").append(++count);
		}
		PanacheQuery<Todo> query;
		if (StringUtils.isNotBlank(queryString)) {
			query = find(queryString.toString(), plannedEndDate, done);
		} else {
			query = findAll();
		}
		return query
				.stream()
				.map(t -> (Todo) t)
				.collect(Collectors.groupingBy(t -> t.category,
						TreeMap::new,
						Collectors.mapping(t -> t, Collectors.toList())));
	}
}
