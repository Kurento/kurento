package com.kurento.kmf.media.events;

import com.kurento.kmf.media.MediaObject;
import com.kurento.tool.rom.server.Param;

public class MediaEvent implements Event {

	private MediaObject source;
	private String type;

	public MediaEvent(@Param("source") MediaObject source,
			@Param("type") String type) {
		super();
		this.source = source;
		this.type = type;
	}

	public MediaObject getSource() {
		return source;
	}

	public void setSource(MediaObject source) {
		this.source = source;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

}
