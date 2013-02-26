package com.kurento.kms.media;

import java.io.Serializable;

public class MediaFactory  implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static MediaPlayer getMediaPlayer(String uri){
		return null;
	}
	
	public static MediaRecorder getMediaRecorder (String uri){
		return null;
	}
	
	public static Stream getStream() {
		return null;
	}
	
	public  static <T extends Mixer> T getMixer(Class<T> clazz){
		return null;
	}
	

}
