package org.kurento.client.test.modules;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.kurento.client.EventListener;
import org.kurento.client.ListenerSubscription;
import org.kurento.module.markerdetector.ArMarkerdetector;
import org.kurento.module.markerdetector.MarkerCountEvent;

public class MarkerDetectorApiTest extends FilterApiBaseTest<ArMarkerdetector> {

	public void addRemoveMarkerCountListenerTest() {
		ListenerSubscription subscription = filter
				.addMarkerCountListener(new EventListener<MarkerCountEvent>() {

					@Override
					public void onEvent(MarkerCountEvent event) {
						// Intentionally left blank
					}
				});
		filter.removeMarkerCountListener(subscription);
	}

	@Override
	@Before
	public void builderTest() {
		filter = new ArMarkerdetector.Builder(pipeline).build();
		assertThat(filter, is(notNullValue()));
	}

	@Test
	public void getSetOverlayScaleTest() {
		float scale = filter.getOverlayScale();
		filter.setOverlayScale(scale + 100);
		assertThat(filter.getOverlayScale(), is(scale + 100));
	}

	@Test
	public void getSetOverlayTextTest() {
		String text = "Overlay text";
		filter.setOverlayText(text);
		assertThat(filter.getOverlayText(), is(text));

		String anotherText = "Another overlay text";
		filter.setOverlayText(anotherText);
		assertThat(filter.getOverlayText(), is(anotherText));
	}

	@Test
	public void getSetShowDebugLevelTest() {
		int level = filter.getShowDebugLevel();
		filter.setShowDebugLevel(level + 1);
		assertThat(filter.getShowDebugLevel(), is(level + 1));
	}

}
