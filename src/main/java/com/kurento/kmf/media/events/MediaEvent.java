package com.kurento.kmf.media.events;

import com.kurento.kmf.media.MediaObject;

public interface MediaEvent {

	public MediaObject getSource();

	public String getType();

}
