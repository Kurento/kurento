package com.kurento.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.content.ContentException;
import com.kurento.kmf.content.RecordRequest;
import com.kurento.kmf.content.RecorderHandler;
import com.kurento.kmf.content.RecorderService;

@RecorderService(name = "MyRecorderHandlerRecordWithRedirect", path = "/recorder-record-with-redirect", redirect = true)
public class MyRecorderHandlerRecordWithRedirect implements RecorderHandler {

	private static final Logger log = LoggerFactory
			.getLogger(MyRecorderHandlerRecordWithRedirect.class);

	@Override
	public void onRecordRequest(RecordRequest recordRequest)
			throws ContentException {
		log.debug("onRecordRequest");
		recordRequest.record("myfile-with-redirect");
	}

	@Override
	public void onContentRecorded(String contentId) {
		log.debug("onContentRecorded");
	}

	@Override
	public void onContentError(String contentId, ContentException exception) {
		log.debug("onContentError " + exception.getMessage());
	}
}
