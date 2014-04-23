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

import com.google.gson.annotations.SerializedName;

/**
 * Query context response object
 * 
 * @author Ivan Gracia (izanmail@gmail.com)
 */
public class QueryContextResponse extends AbstractOrionResponse {

	/**
	 * 
	 */
	@SerializedName("contextElement")
	private OrionContextElement element;

	/**
	 * 
	 */
	public QueryContextResponse() {
	}

	/**
	 * @return the element
	 */
	public OrionContextElement getElement() {
		return element;
	}

	/**
	 * @param element
	 *            the element to set
	 */
	public void setElement(OrionContextElement element) {
		this.element = element;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append(element).append("\n");
		sb.append(super.toString());

		return sb.toString();
	}
}