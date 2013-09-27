package com.kurento.kmf.media;

import java.util.Collection;

public interface MediaSource extends MediaPad {
	/**
	 * Connects the current source with a {@link MediaSink}
	 * 
	 * @param sink
	 *            The sink to connect this source
	 */
	public void connect(MediaSink sink);

	/**
	 * Gets all {@link MediaSink} to which this source is connected.
	 * 
	 * @return The list of sinks that the source is connected to.
	 */
	public Collection<MediaSink> getConnectedSinks();

	/**
	 * Connects the current source with a {@link MediaSink}
	 * 
	 * @param sink
	 *            The sink to connect this source
	 */
	public void connect(MediaSink sink, final Continuation<Void> cont);

	/**
	 * Gets all {@link MediaSink} to which this source is connected.
	 * 
	 * @return The list of sinks that the source is connected to.
	 */
	public void getConnectedSinks(final Continuation<Collection<MediaSink>> cont);
}