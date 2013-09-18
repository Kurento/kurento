package com.kurento.kmf.content;

import com.kurento.kmf.media.MediaElement;

/**
 * TODO: review & improve javadoc
 * 
 * Defines the operations performed by the RecordRequest object, which is in
 * charge of the recording a content in a Media Server.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Miguel París (mparisdiaz@gsyc.es)
 * @author Boni García (bgarcia@gsyc.es)
 * @version 1.0.0
 */
public interface HttpRecorderSession extends ContentSession {

	/**
	 * Performs a record action of media elements. TODO: review & improve
	 * javadoc
	 * 
	 * @param element
	 *            pluggable media component
	 * @throws ContentException
	 *             Exception in the record
	 */
	public void start(String contentPath);

	/**
	 * Performs a record action of media elements. TODO: review & improve
	 * javadoc
	 * 
	 * @param element
	 *            pluggable media component
	 * @throws ContentException
	 *             Exception in the record
	 */
	public void start(MediaElement... sinks);

	public void start(MediaElement sink);
}
