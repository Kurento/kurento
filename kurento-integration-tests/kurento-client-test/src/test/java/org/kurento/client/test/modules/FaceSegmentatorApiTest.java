package org.kurento.client.test.modules;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.kurento.client.EventListener;
import org.kurento.client.ListenerSubscription;
import org.kurento.module.facesegmentator.EyesPositionEvent;
import org.kurento.module.facesegmentator.FacePositionEvent;
import org.kurento.module.facesegmentator.FaceSegmentatorFilter;
import org.kurento.module.facesegmentator.MouthPositionEvent;
import org.kurento.module.facesegmentator.NoisePositionEvent;

public class FaceSegmentatorApiTest extends
		FilterApiBaseTest<FaceSegmentatorFilter> {

	public void addRemoveEyesPositionListenerTest() {
		ListenerSubscription listenerSubscription = filter
				.addEyesPositionListener(new EventListener<EyesPositionEvent>() {
					@Override
					public void onEvent(EyesPositionEvent event) {
						// intentionally left blank
					}
				});
		filter.removeEyesPositionListener(listenerSubscription);
	}

	@Test
	public void addRemoveFacePositionListenerTest() {
		ListenerSubscription listenerSubscription = filter
				.addFacePositionListener(new EventListener<FacePositionEvent>() {
					@Override
					public void onEvent(FacePositionEvent event) {
						// intentionally left blank
					}
				});
		filter.removeFacePositionListener(listenerSubscription);
	}

	@Test
	public void addRemoveMouthPositionListenerTest() {
		ListenerSubscription listenerSubscription = filter
				.addMouthPositionListener(new EventListener<MouthPositionEvent>() {
					@Override
					public void onEvent(MouthPositionEvent event) {
						// intentionally left blank
					}
				});
		filter.removeMouthPositionListener(listenerSubscription);
	}

	@Test
	public void addRemoveNoisePositionListenerTest() {
		ListenerSubscription listenerSubscription = filter
				.addNoisePositionListener(new EventListener<NoisePositionEvent>() {
					@Override
					public void onEvent(NoisePositionEvent event) {
						// intentionally left blank
					}
				});
		filter.removeNoisePositionListener(listenerSubscription);
	}

	@Override
	@Before
	public void builderTest() {
		filter = new FaceSegmentatorFilter.Builder(pipeline).build();
	}

	@Test
	public void getSetIntervalEventTimeTest() {
		int originalTime = filter.getIntervalEventTime();
		filter.setIntervalEventTime(originalTime + 100);
		assertThat(filter.getIntervalEventTime(), is(originalTime + 100));
	}

}
