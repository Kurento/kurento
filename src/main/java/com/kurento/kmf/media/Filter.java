package com.kurento.kmf.media;

import java.lang.reflect.Field;

import com.kurento.kms.api.FilterType;
import com.kurento.kms.api.MediaObject;

public abstract class Filter extends MediaElement {

	private static final long serialVersionUID = 1L;

	private static final String FILTER_TYPE_FIELD_NAME = "filterType";

	Filter(MediaObject filter) {
		super(filter);
	}

	static <T extends Filter> FilterType getType(Class<T> type) {
		try {
			Field field = type.getDeclaredField(FILTER_TYPE_FIELD_NAME);
			return (FilterType) field.get(type);
		} catch (NoSuchFieldException e) {
			throw new IllegalArgumentException(e);
		} catch (SecurityException e) {
			throw new IllegalArgumentException(e);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(e);
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException(e);
		}
	}

}
