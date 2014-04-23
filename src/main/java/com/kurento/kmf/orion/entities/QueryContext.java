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
package com.kurento.kmf.orion.entities;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.gson.annotations.SerializedName;

/**
 * Query context request object
 * 
 * @author Ivan Gracia (izanmail@gmail.com)
 */
public class QueryContext {
	/**
	 * 
	 */
	@SerializedName("entities")
	private List<OrionContextElement> entities;
	/**
	 * 
	 */
	@SerializedName("updateAction")
	private List<OrionAttribute<?>> attributes;

	public QueryContext(List<OrionContextElement> entities,
			OrionAttribute<?>... attributes) {
		this.attributes = ImmutableList.copyOf(attributes);
		this.entities = ImmutableList.copyOf(entities);
	}

	public QueryContext(List<OrionContextElement> entities) {
		this.entities = ImmutableList.copyOf(entities);
		this.attributes = ImmutableList.of();
	}

	public QueryContext(OrionContextElement entity) {
		this.entities = ImmutableList.of(entity);
		this.attributes = ImmutableList.of();
	}

	/**
	 * @return the entities
	 */
	public List<OrionContextElement> getEntities() {
		return entities;
	}

	/**
	 * @param entities
	 *            the entities to set
	 */
	public void setEntities(List<OrionContextElement> entities) {
		this.entities = entities;
	}

	/**
	 * @return the attributes
	 */
	public List<OrionAttribute<?>> getAttributes() {
		return attributes;
	}

	/**
	 * @param attributes
	 *            the attributes to set
	 */
	public void setAttributes(List<OrionAttribute<?>> attributes) {
		this.attributes = attributes;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		for (OrionContextElement element : entities) {
			sb.append(element).append("\n");
		}

		return sb.toString();
	}
}
