package org.kurento.client.test.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kurento.client.ErrorEvent;
import org.kurento.client.EventListener;
import org.kurento.client.ListenerSubscription;
import org.kurento.client.MediaObject;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.client.test.util.MediaPipelineBaseTest;

/**
 * Test class for methods in {@link MediaObject}
 *
 * @author Ivan Gracia (igracia@kurento.org)
 * @since 5.1.0
 *
 */
public class MediaObjectApiTest extends MediaPipelineBaseTest {

	private WebRtcEndpoint webrtcEP;

	@Before
	public void setupMediaElements() {
		webrtcEP = new WebRtcEndpoint.Builder(pipeline).build();
	}

	@After
	public void teardownMediaElements() {
		webrtcEP.release();
	}

	/**
	 * Test the method {@link MediaObject#getMediaPipeline()}. Asserts that
	 * <ul>
	 * <li>A media element has the pipeline correctly set</li>
	 * <li>A pipeline has himself as pipeline</li>
	 * </ul>
	 *
	 */
	@Test
	public void getMediaPipelineTest() {
		assertEquals(pipeline, webrtcEP.getMediaPipeline());
		assertEquals(pipeline, pipeline.getMediaPipeline());
	}

	/**
	 * Test the method {@link MediaObject#getParent()}. Asserts that
	 * <ul>
	 * <li>A media element has a pipeline as parent</li>
	 * <li>A pipeline does not have a parent</li>
	 * </ul>
	 *
	 */
	@Test
	public void getParentTest() {
		assertEquals(pipeline, webrtcEP.getParent());
		assertNull(pipeline.getParent());
	}

	/**
	 * Test the method {@link MediaObject#getIdTest()}. Asserts that the method
	 * works in media elements and pipelines
	 *
	 */
	@Test
	public void getIdTest() {
		assertNotNull(pipeline.getId());
		assertNotNull(webrtcEP.getId());
	}

	/**
	 * Test the method {@link MediaObject#getChildsTest()}. Asserts that
	 * <ul>
	 * <li>the pipeline has one child</li>
	 * <li>the child is indeed the one expected</li>
	 * </ul>
	 *
	 */
	@Test
	public void getChildsTest() {
		List<MediaObject> childs = pipeline.getChilds();
		assertEquals(1, childs.size());
		assertEquals(webrtcEP, childs.get(0));
	}

	/**
	 * Test the method {@link MediaObject#setName()} and
	 * {@link MediaObject#getName()}. Asserts that
	 * <ul>
	 * <li>The method {@link MediaObject#setName()} return OK</li>
	 * <li>The method {@link MediaObject#getName()} return a String</li>
	 * <li>The string returned by get is the same as the one put by set</li>
	 * </ul>
	 *
	 */
	@Test
	public void getSetGetNameTest() {
		String newName = "NewName";
		pipeline.setName(newName);
		assertEquals(newName, pipeline.getName());
	}

	/**
	 * Test the method {@link MediaObject#addErrorListener()} and
	 * {@link MediaObject#removeErrorListener()}. Asserts that
	 * <ul>
	 * <li>the method {@link MediaObject#addErrorListener()} returns a valid
	 * {@link ListenerSubscription}</li>
	 * <li>the method {@link MediaObject#removeErrorListener()} executes
	 * correctly</li>
	 * </ul>
	 *
	 */
	@Test
	public void addAndRemoveErrorListenerTest() {
		ListenerSubscription subscription = pipeline
				.addErrorListener(new EventListener<ErrorEvent>() {
					@Override
					public void onEvent(ErrorEvent event) {
						// Intentionally left blank
					}
				});
		pipeline.removeErrorListener(subscription);
	}

}
