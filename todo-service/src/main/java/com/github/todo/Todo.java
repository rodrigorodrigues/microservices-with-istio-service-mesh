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
import io.quarkus.panache.common.Parameters;
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

	public static Map<Category, List<Todo>> findAllByCategory(Instant plannedEndDate, Boolean done,
			String personId, String categoryName) {
		Parameters parameters = new Parameters();
		StringBuilder query = new StringBuilder();
		if (done != null) {
			query.append(" and done = :done");
			parameters.and("done", done);
		}
		if (plannedEndDate != null) {
			query.append(" and plannedEndDate <= :plannedEndDate");
			parameters.and("plannedEndDate", plannedEndDate);
		}
		if (StringUtils.isNotBlank(personId)) {
			query.append(" and personId = :personId");
			parameters.and("personId", personId);
		}
		if (StringUtils.isNotBlank(categoryName)) {
			query.append(" and category like :category");
			parameters.and("category", categoryName);
		}
		PanacheQuery<Todo> panacheQuery;
		String queryString = query.toString();
		if (StringUtils.isNotBlank(queryString)) {
			panacheQuery = find(queryString.replaceFirst(" and ", ""), parameters);
		} else {
			panacheQuery = findAll();
		}
		return panacheQuery
				.stream()
				.collect(Collectors.groupingBy(t -> t.category,
						TreeMap::new,
						Collectors.mapping(t -> t, Collectors.toList())));
	}
}
