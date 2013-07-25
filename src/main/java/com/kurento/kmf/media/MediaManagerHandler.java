package com.kurento.kmf.media;

public interface MediaManagerHandler {

	void onError(KmsError error);

	void onEvent(KmsEvent event);
}
