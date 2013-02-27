package com.kurento.kms.media;

import java.io.Serializable;

public class MediaFactory implements Serializable {

	private static final long serialVersionUID = 1L;

	public MediaPlayer getMediaPlayer(String uri) {
		return null;
	}

	public MediaRecorder getMediaRecorder(String uri) {
		return null;
	}

	public Stream getStream() {
		return null;
	}

	public <T extends Mixer> T getMixer(Class<T> clazz) {
		return null;
	}

}
