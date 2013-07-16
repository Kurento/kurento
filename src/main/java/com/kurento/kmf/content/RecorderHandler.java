package com.kurento.kmf.content;

public interface RecorderHandler {

	void onRecordRequest(RecordRequest recordRequest);

	void onContentRecorded(String contentId);

	void onError(String contentId, ContentException exception);

}
