package org.kurento.room.server.app;

public class User {

	private String name;
	private String role;

	public User(String name, String role) {
		super();
		this.name = name;
		this.role = role;
	}

	public String getName() {
		return name;
	}

	public String getRole() {
		return role;
	}

}
