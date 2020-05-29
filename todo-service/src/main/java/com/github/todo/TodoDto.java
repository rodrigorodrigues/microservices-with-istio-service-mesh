package com.github.todo;

import java.time.Instant;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class TodoDto {
	@NotBlank
	private String name;
	@NotBlank
	private String personId;
	@NotBlank
	private String personName;
	private boolean activated;
	@NotNull
	private Instant lastModifiedDate = Instant.now();

	TodoDto() {}

	TodoDto(@NotBlank String name, @NotBlank String personId, @NotBlank String personName, boolean activated, @NotNull Instant lastModifiedDate) {
		this.name = name;
		this.personId = personId;
		this.personName = personName;
		this.activated = activated;
		this.lastModifiedDate = lastModifiedDate;
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

	public boolean isActivated() {
		return activated;
	}

	public void setActivated(boolean activated) {
		this.activated = activated;
	}

	public Instant getLastModifiedDate() {
		return lastModifiedDate;
	}

	public void setLastModifiedDate(Instant lastModifiedDate) {
		this.lastModifiedDate = lastModifiedDate;
	}

	public void setPersonName(String personName) {
		this.personName = personName;
	}

	public String getPersonName() {
		return personName;
	}
}
