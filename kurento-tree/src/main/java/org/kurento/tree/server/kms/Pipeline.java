package org.kurento.tree.server.kms;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Iterables;

public class Pipeline extends KurentoObj {

	protected Kms kms;
	protected List<WebRtc> webRtcs = new ArrayList<>();
	protected List<Plumber> plumbers = new ArrayList<>();

	public Pipeline(Kms kms) {
		this.kms = kms;
	}

	public Kms getKms() {
		return kms;
	}

	public WebRtc createWebRtc() {
		WebRtc webRtc = newWebRtc();
		webRtcs.add(webRtc);
		return webRtc;
	}

	public Plumber createPlumber() {
		Plumber plumber = newPlumber();
		plumbers.add(plumber);
		return plumber;
	}

	public List<WebRtc> getWebRtcs() {
		return webRtcs;
	}

	public List<Plumber> getPlumbers() {
		return plumbers;
	}

	public Iterable<Element> getElements() {
		return Iterables.concat(webRtcs, plumbers);
	}

	public Plumber[] link(Pipeline sinkPipeline) {

		Plumber sourcePipelinePlumber = this.createPlumber();
		Plumber sinkPipelinePlumber = sinkPipeline.createPlumber();

		sourcePipelinePlumber.link(sinkPipelinePlumber);

		return new Plumber[] { sourcePipelinePlumber, sinkPipelinePlumber };
	}

	protected WebRtc newWebRtc() {
		return new WebRtc(this);
	}

	protected Plumber newPlumber() {
		return new Plumber(this);
	}
}
