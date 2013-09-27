package com.kurento.kmf.media.internal.spring;

import com.kurento.kmf.media.internal.ProvidesMediaCommand;
import com.kurento.kmf.media.internal.ProvidesMediaCommandResult;

class MediaCommandResultClassStore extends
		AbstractAnnotationClassStore<ProvidesMediaCommandResult> {

	MediaCommandResultClassStore() {
		super(ProvidesMediaCommandResult.class);
	}

	@Override
	protected String getTypeFromAnnotation(Class<?> clazz) {
		ProvidesMediaCommand annotation = clazz
				.getAnnotation(ProvidesMediaCommand.class);
		return annotation.type();
	}

}
