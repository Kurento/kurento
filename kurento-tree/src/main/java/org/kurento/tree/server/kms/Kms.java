package org.kurento.tree.server.kms;

import java.util.ArrayList;
import java.util.List;

public class Kms extends KurentoObj {

	protected List<Pipeline> pipelines = new ArrayList<>();
	private LoadManager loadManager = new MaxWebRtcLoadManager(10000);

	public Kms() {

	}

	public Kms(String label) {
		super(label);
	}

	public Pipeline createPipeline() {
		Pipeline pipeline = newPipeline();
		pipelines.add(pipeline);
		return pipeline;
	}

	public List<Pipeline> getPipelines() {
		return pipelines;
	}

	public void setLoadManager(LoadManager loadManager) {
		this.loadManager = loadManager;
	}

	protected Pipeline newPipeline() {
		return new Pipeline(this);
	}

	public double getLoad() {
		return loadManager.calculateLoad(this);
	}

	public boolean allowMoreElements() {
		return loadManager.allowMoreElements(this);
	}

}
