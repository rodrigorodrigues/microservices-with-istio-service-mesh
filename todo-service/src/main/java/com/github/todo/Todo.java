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
import org.bson.Document;

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

	public static Map<Category, List<Todo>> findAllByCategory(Instant plannedEndDate, Boolean done, String personId) {
		Document document = new Document();
		if (done != null) {
			document.append("done", done);
		}
		if (plannedEndDate != null) {
			document.append("plannedEndDate", plannedEndDate);
		}
		if (StringUtils.isNotBlank(personId)) {
			document.append("personId", personId);
		}
		PanacheQuery<Todo> query;
		if (document.size() > 0) {
			query = find(document);
		} else {
			query = findAll();
		}
		return query
				.stream()
				.collect(Collectors.groupingBy(t -> t.category,
						TreeMap::new,
						Collectors.mapping(t -> t, Collectors.toList())));
	}
}
