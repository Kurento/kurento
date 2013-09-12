package com.kurento.kmf.content;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 
 * Annotation for the implementation of a RtpMediaHandler; it should be used in
 * conjunction within the implementation of the {@link RtpMediaHandler}
 * interface. The following snippet shows an skeleton with the implementation of
 * a RTP Handler:
 * 
 * <pre>
 * &#064;RtpMediaService(name = &quot;MyRtpHandler&quot;, path = &quot;/my-rtp-media&quot;)
 * public class MyRtpMediaHandler implements RtpMediaHandler {
 * 
 * 	&#064;Override
 * 	public void onMediaRequest(RtpMediaRequest request) throws ContentException {
 * 		// My implementation
 * 	}
 * 
 * 	&#064;Override
 * 	public void onMediaTerminated(String requestId) {
 * 		// My implementation
 * 	}
 * 
 * 	&#064;Override
 * 	public onMediaError(String requestId, ContentException exception) {
 * 		// My implementation
 * 	}
 * 
 * }
 * </pre>
 * 
 * @see RtpMediaHandler
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface WebRtcMediaService {
	/**
	 * Name of the WebRTC Handler; this name MUST be unique; in other words, in
	 * several handlers exists within the same application, each of them must
	 * have a different name.
	 * 
	 */
	String name();

	/**
	 * The handler will be instrumented as a HTTP Servlet in the application
	 * server; this parameter establishes the path of the servlet; the same way
	 * as the name, if several handlers co-exists within the same application,
	 * the paths must be also different.
	 */
	String path();
}
