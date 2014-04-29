package com.kurento.kmf.orion.test;

import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.orion.OrionConnector;
import com.kurento.kmf.orion.OrionConnectorConfiguration;
import com.kurento.kmf.orion.entities.ContextUpdateResponse;
import com.kurento.kmf.orion.entities.OrionAttribute;
import com.kurento.kmf.orion.entities.OrionContextElement;

public class BasicEchoTest {

	private static final Logger log = LoggerFactory
			.getLogger(BasicEchoTest.class);

	@Test
	public void test() {

		OrionContextElement oer1 = new OrionContextElement();
		oer1.setId("12");
		oer1.setPattern(false);
		oer1.setType("test");

		OrionAttribute<String> oa1 = new OrionAttribute<>("oa1", "oa1", "temp");
		OrionAttribute<String> oa2 = new OrionAttribute<>("oa2", "oa2",
				"another temp");

		List<OrionAttribute<String>> oaList = newArrayList(oa1, oa2);
		oer1.getAttributes().addAll(oaList);

		OrionContextElement oer2 = new OrionContextElement();
		oer2.setId("2");
		oer2.setPattern(false);
		oer2.setType("test2");

		OrionAttribute<String> oa3 = new OrionAttribute<>("oa3", "oa3", "temp3");
		OrionAttribute<String> oa4 = new OrionAttribute<>("oa4", "oa4", "temp4");

		List<OrionAttribute<String>> oaList2 = newArrayList(oa3, oa4);
		oer2.getAttributes().addAll(oaList2);

		OrionConnector op = new OrionConnector(
				new OrionConnectorConfiguration());

		ContextUpdateResponse resp = op.registerContextElements(oer1, oer2);

		log.info("Response: {}", resp);
		resp = op.deleteContextElements(oer1);
		log.info("Response: {}", resp);
		resp = op.deleteContextElements(oer2);
		log.info("Response: {}", resp);
		log.info("Test finished");
	}

	@Test
	public void testWithZbarMediaObject() {
		OrionConnector op = new OrionConnector(
				new OrionConnectorConfiguration());

		final OrionContextElement ctxElement = new OrionContextElement();
		ctxElement.setId("ZBarHandler_test");
		ctxElement.setType("ZBarFilter");
		ctxElement.getAttributes().add(
				new OrionAttribute<>("CodeFoundEvent", "CodeFoundEvent",
						"xxxxx"));
		ContextUpdateResponse resp = op.registerContextElements(ctxElement);

		log.info("Response: {}", resp);

		resp = op.deleteContextElements(ctxElement);
		log.info("Response: {}", resp);

		log.info("Test finished");
	}
}
