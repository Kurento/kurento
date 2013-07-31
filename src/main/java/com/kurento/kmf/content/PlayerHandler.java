package com.kurento.kmf.content;

public interface PlayerHandler {

	void onPlayRequest(PlayRequest playRequest) throws ContentException;

	// TODO: only an Id should be provided here. HttpServletRequest may be
	// out of its lifecycle. The same problem exists with recorder. It would be
	// useful to provide the capability of storing attributes at onPlayRequest
	// so that they can be recovered later on these callbacks.
	void onContentPlayed(PlayRequest playRequest);

	// TODO: only an Id should be provided here. HttpServletRequest may be
	// out of its lifecycle. The same problem exists with recorder
	void onContentError(PlayRequest playRequest, ContentException exception);

}
