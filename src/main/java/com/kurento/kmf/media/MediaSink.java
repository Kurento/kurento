package com.kurento.kmf.media;

public interface MediaSink extends MediaPad {
	/**
	 * Disconnects the current sink from the referred {@link MediaSource}
	 * 
	 * @param src
	 *            The source to disconnect
	 */
	void disconnect(MediaSource src);

	/**
	 * Gets the {@link MediaSource} that is connected to this sink.
	 * 
	 * @return The source connected to this sink.
	 */
	MediaSource getConnectedSrc();

	/**
	 * Connects the current source with a {@link MediaSink}
	 * 
	 * @param sink
	 *            The sink to connect this source
	 */
	void disconnect(MediaSink sink, final Continuation<Void> cont);

/**
	 * Gets all {@link MediaSink to which this source is connected.
	 * 
	 * @return The list of sinks that the source is connected to.
	 */
	public void getConnectedSrc(final Continuation<MediaSource> cont);
}
