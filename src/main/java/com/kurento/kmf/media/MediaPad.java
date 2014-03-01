package com.kurento.kmf.media;


public interface MediaPad extends MediaObject {

	MediaElement getMediaElement();

	void getMediaElement(Continuation<MediaElement> cont);

	MediaType getMediaType();

	void getMediaType(Continuation<MediaType> cont);

	String getMediaDescription();

	void getMediaDescription(Continuation<String> cont);

}
