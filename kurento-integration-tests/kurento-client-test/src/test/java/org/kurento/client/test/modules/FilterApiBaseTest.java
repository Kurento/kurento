package org.kurento.client.test.modules;

import org.kurento.client.Filter;

public abstract class FilterApiBaseTest<T extends Filter> extends
		MediaElementApiBaseTest<T> {

	protected T filter;

	public abstract void builderTest();

	@Override
	public T getMediaObject() {
		return this.filter;
	}

}
