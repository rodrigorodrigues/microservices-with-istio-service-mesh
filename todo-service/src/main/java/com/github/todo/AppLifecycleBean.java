package com.github.todo;

import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.mongodb.client.MongoClient;
import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class AppLifecycleBean {
	private static final Logger log = LoggerFactory.getLogger(AppLifecycleBean.class);

	@ConfigProperty(name = "configuration.initialLoad", defaultValue = "true")
	boolean loadMockedData;

	@Inject
	MongoClient mongoClient;

	void onStart(@Observes StartupEvent ev) {
		if (loadMockedData) {
			log.debug("MongoDB settings: {}", mongoClient.getClusterDescription());
			if (Todo.count() == 0) {
				Todo todo = new Todo();
				todo.name = "Learn Quarkus";
				todo.personId = "default@admin.com";
				Todo todo1 = new Todo();
				todo1.name = "Learn Kotlin";
				todo1.personId = "default@admin.com";
				Todo todo2 = new Todo();
				todo2.name = "Learn Hurling";
				todo2.personId = "default@admin.com";
				Todo.persist(Stream.of(todo, todo1, todo2));
			}
		}
	}

}
