package com.kurento.kms.media;


public interface MediaPlayer extends Joinable, MediaResource {
	
	void play();
	void pause();
	void stop();

}
