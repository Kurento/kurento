package org.kurento.tree.server.kms;

import java.util.ArrayList;
import java.util.List;

public abstract class Element extends KurentoObj {

	private Pipeline pipeline;
	private List<Element> sinks = new ArrayList<>();
	private Element source;

	public Element(Pipeline pipeline) {
		this.pipeline = pipeline;
	}

	public void connect(Element element) {

		checkReleased();

		if (this.getPipeline() != element.getPipeline()) {
			throw new RuntimeException(
					"Elements from different pipelines can not be connected");
		}

		this.sinks.add(element);
		element.setSource(this);
	}

	public void disconnect() {

		checkReleased();

		if (source != null) {
			this.source.sinks.remove(this);
		}
		this.source = null;
	}

	void setSource(Element source) {
		checkReleased();
		this.source = source;
	}

	public List<Element> getSinks() {
		checkReleased();
		return sinks;
	}

	public Pipeline getPipeline() {
		checkReleased();
		return pipeline;
	}

	public Element getSource() {
		checkReleased();
		return source;
	}

	public void release() {
		disconnect();
		for (Element element : new ArrayList<>(getSinks())) { // To avoid
																// concurrent
																// modification
																// exception
			element.disconnect();
		}
		pipeline.removeElement(this);
		super.release();
	}

}
