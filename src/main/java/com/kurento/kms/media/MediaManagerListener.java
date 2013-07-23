package com.kurento.kms.media;

public interface MediaManagerListener {

	void onError(KmsError error);

	void onEvent(KmsEvent event);
}
