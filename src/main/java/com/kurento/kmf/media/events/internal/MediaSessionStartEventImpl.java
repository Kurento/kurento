package com.kurento.kmf.media.events.internal;

import com.kurento.kmf.media.events.MediaSessionStartEvent;
import com.kurento.kmf.media.internal.ProvidesMediaElement;
import com.kurento.kms.thrift.api.KmsEvent;
import com.kurento.kms.thrift.api.mediaEventDataTypesConstants;

@ProvidesMediaElement(type = mediaEventDataTypesConstants.MEDIA_SESSION_START)
public class MediaSessionStartEventImpl extends VoidMediaEvent implements
		MediaSessionStartEvent {

	public MediaSessionStartEventImpl(KmsEvent event) {
		super(event);
	}

}
