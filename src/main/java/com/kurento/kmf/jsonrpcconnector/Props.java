package com.kurento.kmf.jsonrpcconnector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Props implements Iterable<Prop> {

	private Map<String, Object> props;

	public Props() {
		props = new HashMap<String, Object>();
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
