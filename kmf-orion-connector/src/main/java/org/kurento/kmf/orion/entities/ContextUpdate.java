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

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.gson.annotations.SerializedName;

/**
 * Context update request object
 * 
 * @author Ivan Gracia (izanmail@gmail.com)
 */
public class ContextUpdate {

	public static enum ContextUpdateAction {
		UPDATE, DELETE, APPEND;
	}

	@SerializedName("contextElements")
	private List<OrionContextElement> contextElements;

	@SerializedName("updateAction")
	private ContextUpdateAction updateAction;

	public ContextUpdate(ContextUpdateAction updateAction,
			OrionContextElement... contextElements) {
		this.updateAction = updateAction;
		this.contextElements = ImmutableList.copyOf(contextElements);
	}

	/**
	 * @return the contextElements
	 */
	public List<OrionContextElement> getContextElements() {
		return contextElements;
	}

	/**
	 * @param contextElements
	 *            the contextElements to set
	 */
	public void setContextElements(List<OrionContextElement> contextElements) {
		this.contextElements = contextElements;
	}

	/**
	 * @return the updateAction
	 */
	public ContextUpdateAction getUpdateAction() {
		return updateAction;
	}

	/**
	 * @param updateAction
	 *            the updateAction to set
	 */
	public void setUpdateAction(ContextUpdateAction updateAction) {
		this.updateAction = updateAction;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		for (OrionContextElement element : contextElements) {
			sb.append(element).append("\n");
		}
		sb.append(updateAction);
		return sb.toString();
	}
}