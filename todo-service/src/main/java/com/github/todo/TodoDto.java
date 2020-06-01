package com.github.todo;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class TodoDto {
	@NotBlank
	private String name;
	@NotBlank
	private String personId;
	private String personName;
	private Boolean done;
	@NotNull
	private Instant createdDate = Instant.now();
	@NotNull
	private Instant plannedEndDate = Instant.now().plusSeconds(TimeUnit.DAYS.toSeconds(1));
	@NotNull
	private Category category;

	TodoDto() {}

	TodoDto(@NotBlank String name, @NotBlank String personId, @NotBlank String personName, Boolean done, @NotNull Instant createdDate, @NotNull Instant plannedEndDate, @NotNull Category category) {
		this.name = name;
		this.personId = personId;
		this.personName = personName;
		this.done = done;
		this.createdDate = createdDate;
		this.plannedEndDate = plannedEndDate;
		this.category = category;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPersonId() {
		return personId;
	}

	public void setPersonId(String personId) {
		this.personId = personId;
	}

	public Boolean getDone() {
		return done;
	}

	public void setDone(Boolean done) {
		this.done = done;
	}

	public Instant getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Instant createdDate) {
		this.createdDate = createdDate;
	}

	public void setPersonName(String personName) {
		this.personName = personName;
	}

	public String getPersonName() {
		return personName;
	}

	public Instant getPlannedEndDate() {
		return plannedEndDate;
	}

	public void setPlannedEndDate(Instant plannedEndDate) {
		this.plannedEndDate = plannedEndDate;
	}

	public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	@Override
	public String toString() {
		return "TodoDto{" +
				"name='" + name + '\'' +
				", personId='" + personId + '\'' +
				", personName='" + personName + '\'' +
				", done=" + done +
				", createdDate=" + createdDate +
				", plannedEndDate=" + plannedEndDate +
				", category=" + category +
				'}';
	}
}
