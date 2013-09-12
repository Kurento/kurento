package com.kurento.kmf.content;

/**
 * 
 * Defines the events associated to the record operation (
 * {@link #onRecordRequest(RecordRequest)}, {@link #onContentRecorded(String)},
 * and {@link #onContentError(String, ContentException)}); the implementation of
 * the RecorderHandler should be used in conjunction with
 * {@link RecorderService} annotation. The following snippet shows an skeleton
 * with the implementation of a Recorder:
 * 
 * <pre>
 * &#064;RecorderService(name = &quot;MyRecorderHandlerName&quot;, path = &quot;/my-recorder&quot;, redirect = &quot;true&quot;, useControlProtocol = &quot;false&quot;)
 * public class MyRecorderHandlerRecord implements RecorderHandler {
 * 
 * 	&#064;Override
 * 	public void onRecordRequest(RecordRequest recordRequest)
 * 			throws ContentException {
 * 		// My implementation
 * 	}
 * 
 * 	&#064;Override
 * 	public void onContentRecorded(String contentId) {
 * 		// My implementation
 * 	}
 * 
 * 	&#064;Override
 * 	public void onContentError(String contentId, ContentException exception) {
 * 		// My implementation
 * 	}
 * }
 * </pre>
 * 
 * @see RecorderService
 * @author Miguel París (mparisdiaz@gsyc.es)
 * @author Boni García (bgarcia@gsyc.es)
 * @version 1.0.0
 */
public interface RecorderHandler {

	/**
	 * Event raised when the execution of the recorder handler starts.
	 * 
	 * @param recordRequest
	 *            Object that allows recording a content by the Media Server
	 * @throws ContentException
	 *             Exception while the record operation is performed
	 */
	void onRecordRequest(RecordRequest recordRequest) throws ContentException;

	/**
	 * Event raised when the execution of the recorder handler ends.
	 * 
	 * @param contentId
	 *            Media resource public identification
	 */
	void onContentRecorded(String contentId);

	/**
	 * Event raised when the execution of the player handler launches an
	 * exception.
	 * 
	 * @param contentId
	 *            Media resource public identification
	 * @param exception
	 *            Exception while the record operation is performed
	 */
	void onContentError(String contentId, ContentException exception);

}
