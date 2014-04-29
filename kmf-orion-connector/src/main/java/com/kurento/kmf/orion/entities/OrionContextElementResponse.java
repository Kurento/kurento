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
 * Context element with a status code
 * 
 * @author Ivan Gracia (izanmail@gmail.com)
 * 
 */
public class OrionContextElementResponse extends AbstractOrionResponse {

	@SerializedName("contextElement")
	private OrionContextElement contextElement;

	/**
	 * @return the contextElement
	 */
	public OrionContextElement getContextElement() {
		return contextElement;
	}

	/**
	 * @param contextElement
	 *            the contextElement to set
	 */
	public void setContextElement(OrionContextElement contextElement) {
		this.contextElement = contextElement;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append(contextElement).append("\n");
		sb.append(super.toString());

		return sb.toString();
	}
}
