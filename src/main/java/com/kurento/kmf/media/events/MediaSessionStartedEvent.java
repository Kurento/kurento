package com.kurento.kmf.media.events;

import com.kurento.kmf.media.MediaObject;
import com.kurento.tool.rom.server.Param;

public class MediaSessionStartedEvent extends MediaEvent {

	public MediaSessionStartedEvent(@Param("source") MediaObject source,
			@Param("type") String type) {
		super(source, type);
	}

}
