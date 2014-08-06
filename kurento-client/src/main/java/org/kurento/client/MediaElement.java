/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package org.kurento.client;

import java.util.List;

import org.kurento.client.internal.RemoteClass;
import org.kurento.client.internal.server.Param;

/**
 * 
 * Basic building blocks of the media server, that can be interconnected through
 * the API. A {@link MediaElement} is a module that encapsulates a specific
 * media capability. They can be connected to create media pipelines where those
 * capabilities are applied, in sequence, to the stream going through the
 * pipeline. {@link MediaElement} objects are classified by its supported media
 * type (audio, video, etc.) and the flow direction: {@link MediaSource} pads
 * are intended for media delivery while {@link MediaSink MediaSinks} behave as
 * reception points.
 * 
 **/
@RemoteClass
public interface MediaElement extends MediaObject {

	/**
	 * 
	 * Get the {@link MediaSource sources } of this element
	 * 
	 * @return A list of sources. The list will be empty if no sources are
	 *         found. *
	 **/
	List<MediaSource> getMediaSrcs();

	/**
	 * 
	 * Asynchronous version of getMediaSrcs: {@link Continuation#onSuccess} is
	 * called when the action is done. If an error occurs,
	 * {@link Continuation#onError} is called.
	 * 
	 * @see MediaElement#getMediaSrcs
	 * 
	 **/
	void getMediaSrcs(Continuation<List<MediaSource>> cont);

	/**
	 * 
	 * Get the media sources of the given type and description
	 * 
	 * @param mediaType
	 *            One of {@link #MediaType.AUDIO}, {@link #MediaType.VIDEO} or
	 *            {@link #MediaType.DATA}
	 * @param description
	 *            A textual description of the media source. Currently not used,
	 *            aimed mainly for {@link #MediaType.DATA} sources
	 * @return A list of sources. The list will be empty if no sources are
	 *         found. *
	 **/
	List<MediaSource> getMediaSrcs(@Param("mediaType") MediaType mediaType,
			@Param("description") String description);

	/**
	 * 
	 * Asynchronous version of getMediaSrcs: {@link Continuation#onSuccess} is
	 * called when the action is done. If an error occurs,
	 * {@link Continuation#onError} is called.
	 * 
	 * @see MediaElement#getMediaSrcs
	 * 
	 * @param mediaType
	 *            One of {@link #MediaType.AUDIO}, {@link #MediaType.VIDEO} or
	 *            {@link #MediaType.DATA}
	 * @param description
	 *            A textual description of the media source. Currently not used,
	 *            aimed mainly for {@link #MediaType.DATA} sources
	 * 
	 **/
	void getMediaSrcs(@Param("mediaType") MediaType mediaType,
			@Param("description") String description,
			Continuation<List<MediaSource>> cont);

	/**
	 * 
	 * get media sources of the given type
	 * 
	 * @param mediaType
	 *            One of {@link #MediaType.AUDIO}, {@link #MediaType.VIDEO} or
	 *            {@link #MediaType.DATA}
	 * @return A list of sources. The list will be empty if no sources are
	 *         found. *
	 **/
	List<MediaSource> getMediaSrcs(@Param("mediaType") MediaType mediaType);

	/**
	 * 
	 * Asynchronous version of getMediaSrcs: {@link Continuation#onSuccess} is
	 * called when the action is done. If an error occurs,
	 * {@link Continuation#onError} is called.
	 * 
	 * @see MediaElement#getMediaSrcs
	 * 
	 * @param mediaType
	 *            One of {@link #MediaType.AUDIO}, {@link #MediaType.VIDEO} or
	 *            {@link #MediaType.DATA}
	 * 
	 **/
	void getMediaSrcs(@Param("mediaType") MediaType mediaType,
			Continuation<List<MediaSource>> cont);

	/**
	 * 
	 * Get the {@link MediaSink sinks } of this element
	 * 
	 * @return A list of sinks. The list will be empty if no sinks are found. *
	 **/
	List<MediaSink> getMediaSinks();

	/**
	 * 
	 * Asynchronous version of getMediaSinks: {@link Continuation#onSuccess} is
	 * called when the action is done. If an error occurs,
	 * {@link Continuation#onError} is called.
	 * 
	 * @see MediaElement#getMediaSinks
	 * 
	 **/
	void getMediaSinks(Continuation<List<MediaSink>> cont);

	/**
	 * 
	 * A list of sinks of the given :rom:ref:`MediaType`. The list will be empty
	 * if no sinks are found.
	 * 
	 * @param mediaType
	 *            One of {@link #MediaType.AUDIO}, {@link #MediaType.VIDEO} or
	 *            {@link #MediaType.DATA}
	 * @return A list of sinks. The list will be empty if no sinks are found. *
	 **/
	List<MediaSink> getMediaSinks(@Param("mediaType") MediaType mediaType);

	/**
	 * 
	 * Asynchronous version of getMediaSinks: {@link Continuation#onSuccess} is
	 * called when the action is done. If an error occurs,
	 * {@link Continuation#onError} is called.
	 * 
	 * @see MediaElement#getMediaSinks
	 * 
	 * @param mediaType
	 *            One of {@link #MediaType.AUDIO}, {@link #MediaType.VIDEO} or
	 *            {@link #MediaType.DATA}
	 * 
	 **/
	void getMediaSinks(@Param("mediaType") MediaType mediaType,
			Continuation<List<MediaSink>> cont);

	/**
	 * 
	 * A list of sinks of the given :rom:ref:`MediaType`. The list will be empty
	 * if no sinks are found.
	 * 
	 * @param mediaType
	 *            One of {@link #MediaType.AUDIO}, {@link #MediaType.VIDEO} or
	 *            {@link #MediaType.DATA}
	 * @param description
	 *            A textual description of the media source. Currently not used,
	 *            aimed mainly for {@link #MediaType.DATA} sources
	 * @return A list of sinks. The list will be empty if no sinks are found. *
	 **/
	List<MediaSink> getMediaSinks(@Param("mediaType") MediaType mediaType,
			@Param("description") String description);

	/**
	 * 
	 * Asynchronous version of getMediaSinks: {@link Continuation#onSuccess} is
	 * called when the action is done. If an error occurs,
	 * {@link Continuation#onError} is called.
	 * 
	 * @see MediaElement#getMediaSinks
	 * 
	 * @param mediaType
	 *            One of {@link #MediaType.AUDIO}, {@link #MediaType.VIDEO} or
	 *            {@link #MediaType.DATA}
	 * @param description
	 *            A textual description of the media source. Currently not used,
	 *            aimed mainly for {@link #MediaType.DATA} sources
	 * 
	 **/
	void getMediaSinks(@Param("mediaType") MediaType mediaType,
			@Param("description") String description,
			Continuation<List<MediaSink>> cont);

	/**
	 * 
	 * perform {@link #connect(sink,mediaType)} if there is exactly one sink for
	 * the given type, and their mediaDescriptions are the same
	 * 
	 * @param sink
	 *            the target {@link MediaElement} from which {@link MediaSink}
	 *            will be obtained
	 * @param mediaType
	 *            the :rom:enum:`MediaType` of the pads that will be connected
	 * @param mediaDescription
	 *            A textual description of the media source. Currently not used,
	 *            aimed mainly for {@link #MediaType.DATA} sources
	 * 
	 **/
	void connect(@Param("sink") MediaElement sink,
			@Param("mediaType") MediaType mediaType,
			@Param("mediaDescription") String mediaDescription);

	/**
	 * 
	 * Asynchronous version of connect: {@link Continuation#onSuccess} is called
	 * when the action is done. If an error occurs, {@link Continuation#onError}
	 * is called.
	 * 
	 * @see MediaElement#connect
	 * 
	 * @param sink
	 *            the target {@link MediaElement} from which {@link MediaSink}
	 *            will be obtained
	 * @param mediaType
	 *            the :rom:enum:`MediaType` of the pads that will be connected
	 * @param mediaDescription
	 *            A textual description of the media source. Currently not used,
	 *            aimed mainly for {@link #MediaType.DATA} sources
	 * 
	 **/
	void connect(@Param("sink") MediaElement sink,
			@Param("mediaType") MediaType mediaType,
			@Param("mediaDescription") String mediaDescription,
			Continuation<Void> cont);

	/**
	 * 
	 * Connects every {@link MediaSource} of this element belonging to the
	 * specified :rom:enum:`MediaType` to the corresponding {@link MediaSink} of
	 * the target {@link MediaElement}. This method will throw an exception if
	 * any of the following occur: .. * The number of sources for the specified
	 * :rom:enum:`MediaType` in this element is different than the number of
	 * sinks on the target element. * There are duplicate mediaDescriptions on
	 * this' element sources for the specified :rom:enum:`MediaType`. * There
	 * are duplicate mediaDescriptions on target's element sinks for the
	 * specified :rom:enum:`MediaType`. * Target sinks' media descriptions are
	 * different form this sources' media descriptions for the specified
	 * :rom:enum:`MediaType` This method is not transactional. In case of
	 * exception some of this element sources may be connected with target
	 * sinks.
	 * 
	 * @param sink
	 *            the target {@link MediaElement} from which {@link MediaSink}
	 *            will be obtained
	 * @param mediaType
	 *            the :rom:enum:`MediaType` of the pads that will be connected
	 * 
	 **/
	void connect(@Param("sink") MediaElement sink,
			@Param("mediaType") MediaType mediaType);

	/**
	 * 
	 * Asynchronous version of connect: {@link Continuation#onSuccess} is called
	 * when the action is done. If an error occurs, {@link Continuation#onError}
	 * is called.
	 * 
	 * @see MediaElement#connect
	 * 
	 * @param sink
	 *            the target {@link MediaElement} from which {@link MediaSink}
	 *            will be obtained
	 * @param mediaType
	 *            the :rom:enum:`MediaType` of the pads that will be connected
	 * 
	 **/
	void connect(@Param("sink") MediaElement sink,
			@Param("mediaType") MediaType mediaType, Continuation<Void> cont);

	/**
	 * 
	 * perform {@link #connect(sink,mediaType)} for every available
	 * {@link MediaType} in this source
	 * 
	 * @param sink
	 *            the target {@link MediaElement} from which {@link MediaSink}
	 *            will be obtained
	 * 
	 **/
	void connect(@Param("sink") MediaElement sink);

	/**
	 * 
	 * Asynchronous version of connect: {@link Continuation#onSuccess} is called
	 * when the action is done. If an error occurs, {@link Continuation#onError}
	 * is called.
	 * 
	 * @see MediaElement#connect
	 * 
	 * @param sink
	 *            the target {@link MediaElement} from which {@link MediaSink}
	 *            will be obtained
	 * 
	 **/
	void connect(@Param("sink") MediaElement sink, Continuation<Void> cont);

}
