package com.kurento.kmf.media;

import com.kurento.kmf.media.events.EndOfStreamEvent;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.tool.rom.RemoteClass;
import com.kurento.tool.rom.server.Param;

@RemoteClass
public interface HttpPostEndpoint extends HttpEndpoint {

	ListenerRegistration addEndOfStreamListener(
			MediaEventListener<EndOfStreamEvent> listener);

	void addEndOfStreamListener(MediaEventListener<EndOfStreamEvent> listener,
			Continuation<ListenerRegistration> cont);

	public interface Factory {

		public Builder create(
				@Param("mediaPipeline") MediaPipeline mediaPipeline);
	}

	public interface Builder extends AbstractBuilder<HttpPostEndpoint> {

		public Builder withDisconnectionTimeout(int disconnectionTimeout);

		public Builder useEncodedMedia();
	}
}
