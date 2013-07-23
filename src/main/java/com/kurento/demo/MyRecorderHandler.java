package com.kurento.demo;

import com.kurento.kmf.content.ContentException;
import com.kurento.kmf.content.RecordRequest;
import com.kurento.kmf.content.RecorderHandler;
import com.kurento.kmf.content.RecorderService;

@RecorderService(name = "MyRecorderHandler", path = "/upload")
public class MyRecorderHandler implements RecorderHandler {

	@Override
	public void onRecordRequest(RecordRequest recordRequest)
			throws ContentException {
		// TODO: contentPath is not being used yet
		recordRequest.record("myfile");
	}

	@Override
	public void onContentRecorded(String contentId) {
		// TODO: Auto-generated method stub
	}

	@Override
	public void onContentError(String contentId, ContentException exception) {
		// TODO: Auto-generated method stub
	}
}
