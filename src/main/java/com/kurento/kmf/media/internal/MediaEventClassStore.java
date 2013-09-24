package com.kurento.kmf.media.internal;

import com.kurento.kmf.media.IsMediaEvent;

class MediaEventClassStore extends AbstractAnnotationClassStore<IsMediaEvent> {

	MediaEventClassStore() {
		super(IsMediaEvent.class);
	}

	@Override
	protected String getTypeFromAnnotation(Class<?> clazz) {
		IsMediaEvent annotation = clazz.getAnnotation(IsMediaEvent.class);
		return annotation.type();
	}

}