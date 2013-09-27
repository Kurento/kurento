package com.kurento.kmf.media;

import com.kurento.kmf.media.commands.MediaParam;

public interface MediaMixer extends MediaObject {

	public MediaElement createEndPoint();

	public MediaElement createEndPoint(MediaParam params);

	public void createEndPoint(final Continuation<MediaElement> cont);

	public void createEndPoint(MediaParam params,
			final Continuation<MediaElement> cont);
}
