package org.kurento.client.test.modules;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.kurento.client.EventListener;
import org.kurento.client.ListenerSubscription;
import org.kurento.module.platedetector.PlateDetectedEvent;
import org.kurento.module.platedetector.PlateDetectorFilter;

public class PlateDetectorFilterApiTest extends
		FilterApiBaseTest<PlateDetectorFilter> {

	public void addRemovePlateDetectedListenerTest() {
		ListenerSubscription listenerSubscription = filter
				.addPlateDetectedListener(new EventListener<PlateDetectedEvent>() {
					@Override
					public void onEvent(PlateDetectedEvent event) {
						// intentionally left blank
					}
				});
		filter.removePlateDetectedListener(listenerSubscription);
	}

	@Override
	@Before
	public void builderTest() {
		filter = new PlateDetectorFilter.Builder(pipeline).build();
		assertThat(filter, is(notNullValue()));
	}

	public void setPlateWidthPercentageTest() {
		filter.setPlateWidthPercentage(0.5f);
	}

}
