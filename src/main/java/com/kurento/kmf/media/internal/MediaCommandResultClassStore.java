package com.kurento.kmf.media.internal;

import com.kurento.kmf.media.IsMediaCommandResult;

class MediaCommandResultClassStore extends
		AbstractAnnotationClassStore<IsMediaCommandResult> {

	MediaCommandResultClassStore() {
		super(IsMediaCommandResult.class);
	}

	@Override
	protected String getTypeFromAnnotation(Class<?> clazz) {
		IsMediaCommandResult annotation = clazz
				.getAnnotation(IsMediaCommandResult.class);
		return annotation.commandType();
	}

}
