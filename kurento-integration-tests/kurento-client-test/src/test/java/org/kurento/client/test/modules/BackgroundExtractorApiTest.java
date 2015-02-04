package org.kurento.client.test.modules;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.kurento.module.backgroundextractor.BackgroundExtractorFilter;

public class BackgroundExtractorApiTest extends
		FilterApiBaseTest<BackgroundExtractorFilter> {

	@Override
	@Before
	public void builderTest() {
		filter = new BackgroundExtractorFilter.Builder(pipeline).build();
		assertThat(filter, is(notNullValue()));
	}

	/**
	 * Test if the method
	 * {@link BackgroundExtractorFilter#activateProcessing(boolean)} works with
	 * both possible boolean values, with a delay of 500ms between issuing both
	 * commands
	 *
	 * @throws InterruptedException
	 *             if the sleep is interrupted
	 */
	@Test
	public void startStopPorcessingTest() throws InterruptedException {
		filter = new BackgroundExtractorFilter.Builder(pipeline).build();
		filter.activateProcessing(true);
		Thread.sleep(500);
		filter.activateProcessing(false);
	}

}
