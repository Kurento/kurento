package com.kurento.kmf.content;

public interface PlayerHandler {

	void onPlayRequest(PlayRequest playRequest);

	void onContentPlayed(String contentId);

	void onError(String contentId, ContentException exception);

}
