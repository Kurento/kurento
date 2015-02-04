package org.kurento.client.test.modules;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.kurento.client.internal.server.KurentoServerException;
import org.kurento.module.chroma.ChromaFilter;
import org.kurento.module.chroma.WindowParam;

public class ChromaFilterApiTest extends FilterApiBaseTest<ChromaFilter> {

	@Test(expected = KurentoServerException.class)
	public void builderNegativeNumbersTest() {

		int topRightX = 1;
		int topRightY = 1;
		int width = -10;
		int height = -10;
		WindowParam window = new WindowParam(topRightX, topRightY, width,
				height);
		filter = new ChromaFilter.Builder(pipeline, window).build();

	}

	@Override
	@Test
	public void builderTest() {

		int topRightX = 1;
		int topRightY = 1;
		int width = 10;
		int height = 10;
		WindowParam window = new WindowParam(topRightX, topRightY, width,
				height);
		filter = new ChromaFilter.Builder(pipeline, window).build();
		assertThat(filter, is(notNullValue()));

	}

}
