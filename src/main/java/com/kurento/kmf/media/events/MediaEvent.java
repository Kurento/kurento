package com.kurento.kmf.media.events;

import static com.kurento.kmf.media.internal.refs.MediaRefConverter.fromThrift;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.kurento.kmf.media.internal.refs.MediaObjectRefDTO;
import com.kurento.kmf.media.objects.MediaObject;
import com.kurento.kms.thrift.api.KmsEvent;

public abstract class MediaEvent {

	@Autowired
	protected ApplicationContext applicationContext;

	private final MediaObjectRefDTO sourceRef;
	private MediaObject source;
	private final String type;

	// TODO: should not be visible to final developer
	public MediaEvent(KmsEvent event) {
		this.sourceRef = fromThrift(event.source);
		this.type = event.type;
	}

	public MediaObject getSource() {
		if (source == null) {
			source = (MediaObject) applicationContext.getBean("mediaObject",
					sourceRef);
		}
		return source;
	}

	public String getType() {
		return this.type;
	}

	// TODO: should not be visible to final developer
	public abstract void deserializeData(KmsEvent event);

}
