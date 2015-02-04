package org.kurento.client.test.modules;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.kurento.client.EventListener;
import org.kurento.client.ListenerSubscription;
import org.kurento.module.crowddetector.CrowdDetectorDirectionEvent;
import org.kurento.module.crowddetector.CrowdDetectorFilter;
import org.kurento.module.crowddetector.CrowdDetectorFluidityEvent;
import org.kurento.module.crowddetector.CrowdDetectorOccupancyEvent;
import org.kurento.module.crowddetector.RegionOfInterest;
import org.kurento.module.crowddetector.RegionOfInterestConfig;
import org.kurento.module.crowddetector.RelativePoint;

public class CrowdDetectorApiTest extends
		FilterApiBaseTest<CrowdDetectorFilter> {

	@Test
	public void addRemoveCrowdDetectorDirectionListenerTest() {
		ListenerSubscription listenerSubscription = filter
				.addCrowdDetectorDirectionListener(new EventListener<CrowdDetectorDirectionEvent>() {
					@Override
					public void onEvent(CrowdDetectorDirectionEvent event) {
						// intentionally left blank
					}
				});
		filter.removeCrowdDetectorDirectionListener(listenerSubscription);
	}

	@Test
	public void addRemoveCrowdDetectorFluidityListenerTest() {
		ListenerSubscription listenerSubscription = filter
				.addCrowdDetectorFluidityListener(new EventListener<CrowdDetectorFluidityEvent>() {
					@Override
					public void onEvent(CrowdDetectorFluidityEvent event) {
						// intentionally left blank
					}
				});
		filter.removeCrowdDetectorFluidityListener(listenerSubscription);
	}

	@Test
	public void addRemoveCrowdDetectorOccupancyListenerTest() {
		ListenerSubscription listenerSubscription = filter
				.addCrowdDetectorOccupancyListener(new EventListener<CrowdDetectorOccupancyEvent>() {
					@Override
					public void onEvent(CrowdDetectorOccupancyEvent event) {
						// intentionally left blank
					}
				});
		filter.removeCrowdDetectorOccupancyListener(listenerSubscription);
	}

	@Override
	@Before
	public void builderTest() {

		List<RelativePoint> points1 = Arrays.asList(new RelativePoint(1, 1),
				new RelativePoint(1, 1));
		RegionOfInterestConfig roi1Config = new RegionOfInterestConfig();
		RegionOfInterest roi1 = new RegionOfInterest(points1, roi1Config,
				"region 1");

		List<RegionOfInterest> rois = Arrays.asList(roi1);
		filter = new CrowdDetectorFilter.Builder(pipeline, rois).build();
		assertThat(filter, is(notNullValue()));

	}

	@Test
	public void builderTwoRoisTest() {

		List<RelativePoint> points1 = Arrays.asList(new RelativePoint(1, 1),
				new RelativePoint(1, 1));
		RegionOfInterestConfig roi1Config = new RegionOfInterestConfig();
		RegionOfInterest roi1 = new RegionOfInterest(points1, roi1Config,
				"region 1");

		List<RelativePoint> points2 = Arrays.asList(new RelativePoint(1, 1),
				new RelativePoint(1, 1));
		RegionOfInterestConfig roi2Config = new RegionOfInterestConfig();
		RegionOfInterest roi2 = new RegionOfInterest(points2, roi2Config,
				"region 2");

		List<RegionOfInterest> rois = Arrays.asList(roi1, roi2);
		filter = new CrowdDetectorFilter.Builder(pipeline, rois).build();
		assertThat(filter, is(notNullValue()));

	}

	@Test
	public void getSetProcessingWidthTest() {
		int originalWidth = filter.getProcessingWidth();
		filter.setProcessingWidth(originalWidth + 100);
		assertThat(filter.getProcessingWidth(), is(originalWidth + 100));
	}

}
