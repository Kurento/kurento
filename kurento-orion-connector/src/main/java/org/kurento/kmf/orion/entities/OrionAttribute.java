/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package org.kurento.kmf.orion.entities;

/**
 * Attribute to be used in context elements and queries
 * 
 * @author Ivan Gracia (izanmail@gmail.com)
 * @param <T>
 *            The type of attribute
 * 
 */
public class OrionAttribute<T> {

	private String name;
	private String type;
	private T value;

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type
	 *            the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @return the value
	 */
	public T getValue() {
		return value;
	}

	/**
	 * @param value
	 *            the value to set
	 */
	public void setValue(T value) {
		this.value = value;
	}

	/**
	 * @param name
	 * @param type
	 * @param value
	 */
	public OrionAttribute(String name, String type, T value) {
		super();
		this.name = name;
		this.type = type;
		this.value = value;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append(" Name: ").append(name).append("\n");
		sb.append(" Type: ").append(type).append("\n");
		sb.append(" Value: ").append(value).append("\n");

		return sb.toString();
	}
}
