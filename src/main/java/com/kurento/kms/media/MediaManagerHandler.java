package com.kurento.kms.media;

public interface MediaManagerHandler {

	void onError(KmsError error);

	void onEvent(KmsEvent event);
}
