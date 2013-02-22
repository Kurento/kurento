package com.kurento.kmf.media;

import java.util.Collection;

import com.kurento.mediaspec.MediaType;

public interface Joinable {
	
	void join();
		
	Collection<MediaSrc> getMediaSrcs();

	Collection<MediaSink> getMediaSinks();
	
	Collection<MediaSrc> getMediaSrcs(MediaType mediaType);

	Collection<MediaSink> getMediaSinks(MediaType mediaType);

}
