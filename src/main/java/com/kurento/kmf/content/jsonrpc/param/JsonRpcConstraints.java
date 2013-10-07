package com.kurento.kmf.content.jsonrpc.param;

import com.kurento.kmf.content.jsonrpc.Constraints;

/**
 * 
 * Java representation for JSON constraints.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
public class JsonRpcConstraints {

	/**
	 * Audio constraints.
	 */
	private String video;

	/**
	 * Video constraints.
	 */
	private String audio;

	/**
	 * Default constructor.
	 */
	public JsonRpcConstraints() {
	}

	/**
	 * Parameterized constructor.
	 * 
	 * @param video
	 *            Audio constraints
	 * @param audio
	 *            Video constraints
	 */
	public JsonRpcConstraints(String video, String audio) {
		this.video = video;
		this.audio = audio;
	}

	/**
	 * Video constraints accessor (getter), returned as upper case.
	 * 
	 * @return Upper case video constraints
	 */
	public Constraints getVideoContraints() {
		return Constraints.valueOf(getVideo().toUpperCase());
	}

	/**
	 * Audio constraints accessor (getter), returned as upper case.
	 * 
	 * @return Upper case audio constraints
	 */
	public Constraints getAudioContraints() {
		return Constraints.valueOf(getAudio().toUpperCase());
	}

	/**
	 * Video constraints accessor (getter).
	 * 
	 * @return Video constraints
	 */
	String getVideo() {
		return video;
	}

	/**
	 * Video constraints mutator (setter).
	 * 
	 * @param video
	 *            Video constraints
	 */
	void setVideo(String video) {
		this.video = video;
	}

	/**
	 * Audio constraints accessor (getter).
	 * 
	 * @return Audio constraints
	 */
	String getAudio() {
		return audio;
	}

	/**
	 * Audio constraints mutator (setter).
	 * 
	 * @param audio
	 *            Audio constraints
	 */
	void setAudio(String audio) {
		this.audio = audio;
	}
}