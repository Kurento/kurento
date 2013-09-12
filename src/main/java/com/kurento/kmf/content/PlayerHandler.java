package com.kurento.kmf.content;

/**
 * 
 * Defines the events associated to the play operation (
 * {@link #onPlayRequest(PlayRequest)}, {@link #onContentPlayed(PlayRequest)},
 * and {@link #onContentError(PlayRequest, ContentException)}); the
 * implementation of the PlayerHandler should be used in conjunction with
 * {@link PlayerService} annotation. The following snippet shows an skeleton
 * with the implementation of a Player:
 * 
 * <pre>
 * &#064;PlayerService(name = &quot;MyPlayerHandlerName&quot;, path = &quot;/my-player&quot;, redirect = &quot;true&quot;, useControlProtocol = &quot;false&quot;)
 * public class MyPlayerHandler implements PlayerHandler {
 * 
 * 	&#064;Override
 * 	public void onPlayRequest(PlayRequest playRequest) throws ContentException {
 * 		// My implementation
 * 	}
 * 
 * 	&#064;Override
 * 	public void onContentPlayed(PlayRequest playRequest) {
 * 		// My implementation
 * 	}
 * 
 * 	&#064;Override
 * 	public void onContentError(PlayRequest playRequest,
 * 			ContentException exception) {
 * 		// My implementation
 * 	}
 * 
 * }
 * </pre>
 * 
 * @see PlayerService
 * @author Luis López (llopez@gsyc.es)
 * @author Miguel París (mparisdiaz@gsyc.es)
 * @author Boni García (bgarcia@gsyc.es)
 * @version 1.0.0
 */
public interface PlayerHandler {

	/**
	 * Event raised when the execution of the player handler starts.
	 * 
	 * @param playRequest
	 *            Object that allows requesting a content to be retrieved from a
	 *            Media Server using HTTP pseudostreaming
	 * @throws ContentException
	 *             Exception while the play operation is performed
	 */
	void onPlayRequest(PlayRequest playRequest) throws ContentException;

	// TODO: only an Id should be provided here. HttpServletRequest may be
	// out of its lifecycle. The same problem exists with recorder. It would be
	// useful to provide the capability of storing attributes at onPlayRequest
	// so that they can be recovered later on these callbacks.
	/**
	 * Event raised when the execution of the player handler ends.
	 * 
	 * @param playRequest
	 *            Object that allows requesting a content to be retrieved from a
	 *            Media Server using HTTP pseudostreaming
	 */
	void onContentPlayed(PlayRequest playRequest);

	// TODO: only an Id should be provided here. HttpServletRequest may be
	// out of its lifecycle. The same problem exists with recorder
	/**
	 * Event raised when the execution of the player handler launches an
	 * exception.
	 * 
	 * @param playRequest
	 *            Object that allows requesting a content to be retrieved from a
	 *            Media Server using HTTP pseudostreaming
	 * @param exception
	 *            Exception while the play operation is performed
	 */
	void onContentError(PlayRequest playRequest, ContentException exception);

}
