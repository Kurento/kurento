package com.kurento.kmf.content;

public interface RecorderHandler {

	void onRecordRequest(RecordRequest recordRequest) throws ContentException;

	void onContentRecorded(String contentId);

	void onContentError(String contentId, ContentException exception);

}
