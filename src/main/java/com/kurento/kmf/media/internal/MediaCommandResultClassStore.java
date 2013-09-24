package com.kurento.kmf.media.internal;

import com.kurento.kmf.media.IsMediaCommand;
import com.kurento.kmf.media.IsMediaCommandResult;

class MediaCommandResultClassStore extends
		AbstractAnnotationClassStore<IsMediaCommandResult> {

	MediaCommandResultClassStore() {
		super(IsMediaCommandResult.class);
	}

	@Override
	protected String getTypeFromAnnotation(Class<?> clazz) {
		IsMediaCommand annotation = clazz.getAnnotation(IsMediaCommand.class);
		return annotation.type();
	}

}
