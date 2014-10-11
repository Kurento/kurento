package org.kurento.tree.server.kms;

import java.util.ArrayList;
import java.util.List;

public class Kms extends KurentoObj {

	protected List<Pipeline> pipelines = new ArrayList<>();

	public Kms() {

	}

	public Kms(String label) {
		super(label);
	}

	public Pipeline createPipeline() {
		Pipeline pipeline = new Pipeline(this);
		pipelines.add(pipeline);
		return pipeline;
	}

	public List<Pipeline> getPipelines() {
		return pipelines;
	}

}
