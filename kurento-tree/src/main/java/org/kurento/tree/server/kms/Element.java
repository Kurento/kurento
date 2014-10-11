package org.kurento.tree.server.kms;

import java.util.ArrayList;
import java.util.List;

public abstract class Element extends KurentoObj {

	private Pipeline pipeline;
	private List<Element> sinks = new ArrayList<>();
	private Element source;
	private boolean released = false;

	public Element(Pipeline pipeline) {
		this.pipeline = pipeline;
	}

	public void connect(Element element) {

		if (this.getPipeline() != element.getPipeline()) {
			throw new RuntimeException(
					"Elements from different pipelines can not be connected");
		}

		this.sinks.add(element);
		element.setSource(this);
	}

	public void disconnect() {
		this.source.sinks.remove(this);
		this.source = null;
	}

	void setSource(Element source) {
		this.source = source;
	}

	public List<Element> getSinks() {
		return sinks;
	}

	public Pipeline getPipeline() {
		return pipeline;
	}

	public Element getSource() {
		return source;
	}

	public void release() {
		disconnect();
		for (Element element : getSinks()) {
			element.disconnect();
		}
		released = true;
	}
}
