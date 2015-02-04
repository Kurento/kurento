package org.kurento.client.test.modules;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.kurento.module.motionfilter.MotionFilter;

public class MotionFilterApiTest extends FilterApiBaseTest<MotionFilter> {

	public void applyConfTest() {
		filter.applyConf();
	}

	@Override
	@Before
	public void builderTest() {
		filter = new MotionFilter.Builder(pipeline).build();
		assertThat(filter, is(notNullValue()));
	}

	@Test
	public void sendResultToFiltersTest() {
		filter.sendResultToFilters(1);
	}

	@Test
	public void setGridTest() {
		filter.setGrid("Grid string");
	}

	@Test
	public void setMinNumBlocksTest() {
		filter.setMinNumBlocks("Min num blocks");
	}

}
