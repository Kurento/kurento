package org.kurento.room.server.app;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Room {

	private String name;
	private String id;
	private ConcurrentMap<String, User> users = new ConcurrentHashMap<>();
	private ConcurrentMap<String, Object> attributes = new ConcurrentHashMap<>();

	public Room(String name, String id) {
		super();
		this.name = name;
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public String getId() {
		return id;
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public Object putAttribute(String name, Object value) {
		return this.attributes.put(name, value);
	}

	public Object getAttribute(String name) {
		return this.attributes.get(name);
	}

	public Collection<User> getUsers() {
		return Collections.unmodifiableCollection(users.values());
	}

	String addUser(String name, String role) {

		users.put(name, new User(name, role));
		return UUID.randomUUID().toString();
	}

}
