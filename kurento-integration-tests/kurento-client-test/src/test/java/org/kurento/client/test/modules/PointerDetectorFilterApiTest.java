package org.kurento.client.test.modules;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.kurento.module.pointerdetector.PointerDetectorFilter;
import org.kurento.module.pointerdetector.PointerDetectorWindowMediaParam;
import org.kurento.module.pointerdetector.WindowParam;

public class PointerDetectorFilterApiTest extends
		FilterApiBaseTest<PointerDetectorFilter> {

	@Override
	@Test
	public void builderTest() {

		WindowParam calibrationWindow = new WindowParam(1, 1, 10, 10);

		int topRightX = 1;
		int topRightY = 1;
		int width = 10;
		int height = 10;
		PointerDetectorWindowMediaParam window = new PointerDetectorWindowMediaParam(
				"Window 1", topRightX, topRightY, width, height);
		List<PointerDetectorWindowMediaParam> windows = Arrays.asList(window);
		filter = new PointerDetectorFilter.Builder(pipeline, calibrationWindow)
				.withWindows(windows).build();
		assertThat(filter, is(notNullValue()));

	}

}
