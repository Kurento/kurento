package com.kurento.kmf.media;

import java.util.List;

import com.kurento.tool.rom.RemoteClass;
import com.kurento.tool.rom.server.Param;

@RemoteClass
public interface MediaElement extends MediaObject {

	List<MediaSource> getMediaSrcs();

	void getMediaSrcs(Continuation<List<MediaSource>> cont);

	List<MediaSource> getMediaSrcs(@Param("mediaType") MediaType mediaType,
			@Param("description") String description);

	void getMediaSrcs(@Param("mediaType") MediaType mediaType,
			@Param("description") String description,
			Continuation<List<MediaSource>> cont);

	List<MediaSource> getMediaSrcs(@Param("mediaType") MediaType mediaType);

	void getMediaSrcs(@Param("mediaType") MediaType mediaType,
			Continuation<List<MediaSource>> cont);

	List<MediaSink> getMediaSinks();

	void getMediaSinks(Continuation<List<MediaSink>> cont);

	List<MediaSink> getMediaSinks(@Param("mediaType") MediaType mediaType);

	void getMediaSinks(@Param("mediaType") MediaType mediaType,
			Continuation<List<MediaSink>> cont);

	List<MediaSink> getMediaSinks(@Param("mediaType") MediaType mediaType,
			@Param("description") String description);

	void getMediaSinks(@Param("mediaType") MediaType mediaType,
			@Param("description") String description,
			Continuation<List<MediaSink>> cont);

	void connect(@Param("sink") MediaElement sink,
			@Param("mediaType") MediaType mediaType,
			@Param("mediaDescription") String mediaDescription);

	void connect(@Param("sink") MediaElement sink,
			@Param("mediaType") MediaType mediaType,
			@Param("mediaDescription") String mediaDescription,
			Continuation<Void> cont);

	void connect(@Param("sink") MediaElement sink,
			@Param("mediaType") MediaType mediaType);

	void connect(@Param("sink") MediaElement sink,
			@Param("mediaType") MediaType mediaType, Continuation<Void> cont);

	void connect(@Param("sink") MediaElement sink);

	void connect(@Param("sink") MediaElement sink, Continuation<Void> cont);

}
