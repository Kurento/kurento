package com.kurento.kmf.content;


/**
 * TODO: review & improve javadoc
 * 
 * Defines the events associated to the RTP operation (
 * {@link #onMediaRequest(RtpContentSession)},
 * {@link #onMediaTerminated(String)}, and
 * {@link #onMediaError(String, ContentException)}); the implementation of the
 * RtpMediaHandler should be used in conjunction with {@link RtpContentService}
 * annotation. The following snippet shows an skeleton with the implementation
 * of a RtpMedia:
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
 * @see RtpContentService
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
public abstract class RtpContentHandler extends
		ContentHandler<RtpContentSession> {
}
