/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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
package org.kurento.jsonrpcconnector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Props implements Iterable<Prop> {

	private final Map<String, Object> props;

	public Props() {
		props = new HashMap<>();
	}

	public Props(Map<String, Object> props) {
		this.props = props;
	}

	public Props(String name, Object value) {
		this();
		add(name, value);
	}

	public Object getProp(String name) {
		return props.get(name);
	}

	public boolean hasProp(String name) {
		return props.keySet().contains(name);
	}

	public Props add(String property, Object value) {
		props.put(property, value);
		return this;
	}

	@Override
	public Iterator<Prop> iterator() {

		final Iterator<Map.Entry<String, Object>> entries = props.entrySet()
				.iterator();

		Iterator<Prop> propsIterator = new Iterator<Prop>() {

			@Override
			public boolean hasNext() {
				return entries.hasNext();
			}

			@Override
			public Prop next() {
				Map.Entry<String, Object> entry = entries.next();
				return new PropImpl(entry.getKey(), entry.getValue());
			}

			@Override
			public void remove() {
				entries.remove();
			}
		};

		return propsIterator;
	}

	@Override
	public String toString() {
		return props.toString();
	}

}
