package com.kurento.kmf.media.internal;

import com.kurento.kmf.media.IsMediaElement;

class MediaElementClassStore extends
		AbstractAnnotationClassStore<IsMediaElement> {

	MediaElementClassStore() {
		super(IsMediaElement.class);
	}

	@Override
	protected String getTypeFromAnnotation(Class<?> clazz) {
		IsMediaElement annotation = clazz.getAnnotation(IsMediaElement.class);
		return annotation.type();
	}

}
