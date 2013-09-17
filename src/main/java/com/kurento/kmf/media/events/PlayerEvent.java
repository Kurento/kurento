package com.kurento.kmf.media.events;

import com.kurento.kms.thrift.api.MediaEvent;

public class PlayerEvent extends KmsEvent {

	PlayerEvent(MediaEvent event) {
		super(event);
	}

}
