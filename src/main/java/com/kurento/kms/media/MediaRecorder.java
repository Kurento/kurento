package com.kurento.kms.media;

public interface MediaRecorder extends Joinable, MediaResource {

	void record();

	void pause();

	void stop();

}
