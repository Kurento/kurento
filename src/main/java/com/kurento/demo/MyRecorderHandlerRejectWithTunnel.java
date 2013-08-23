package com.kurento.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.content.ContentException;
import com.kurento.kmf.content.RecordRequest;
import com.kurento.kmf.content.RecorderHandler;
import com.kurento.kmf.content.RecorderService;

@RecorderService(name = "MyRecorderHandlerRejectWithTunnel", path = "/recorder-reject-with-tunnel", redirect = false)
public class MyRecorderHandlerRejectWithTunnel implements RecorderHandler {

	private static final Logger log = LoggerFactory
			.getLogger(MyRecorderHandlerRejectWithTunnel.class);

	@Override
	public void onRecordRequest(RecordRequest recordRequest)
			throws ContentException {
		log.debug("onRecordRequest");
		recordRequest.reject(407, "Rejected in recorder handler (with tunnel)");
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
