package com.kurento.kmf.content;

public interface PlayerHandler {

	void onPlayRequest(PlayRequest playRequest) throws ContentException;

	void onContentPlayed(PlayRequest playRequest);

	void onContentError(PlayRequest playRequest, ContentException exception);

}
