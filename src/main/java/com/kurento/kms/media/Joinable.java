package com.kurento.kms.media;

import java.util.Collection;

import com.kurento.mediaspec.MediaType;

public interface Joinable {

	void join(Joinable peer);

	void unjoin(Joinable peer);

	Collection<MediaSrc> getMediaSrcs();

	Collection<MediaSink> getMediaSinks();

	Collection<MediaSrc> getMediaSrcs(MediaType mediaType);

	Collection<MediaSink> getMediaSinks(MediaType mediaType);

}
