package com.kurento.kmf.media.events.internal;

import com.kurento.kmf.media.events.MediaSessionCompleteEvent;
import com.kurento.kmf.media.internal.ProvidesMediaEvent;
import com.kurento.kms.thrift.api.KmsEvent;
import com.kurento.kms.thrift.api.mediaEventDataTypesConstants;

@ProvidesMediaEvent(type = mediaEventDataTypesConstants.MEDIA_SESSION_COMPLETE)
public class MediaSessionCompleteEventImpl extends VoidMediaEvent implements
		MediaSessionCompleteEvent {

	public MediaSessionCompleteEventImpl(KmsEvent event) {
		super(event);
	}

}
