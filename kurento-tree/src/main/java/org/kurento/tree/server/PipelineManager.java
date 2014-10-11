package org.kurento.tree.server;

import org.kurento.client.MediaPipeline;
import org.kurento.client.PlumberEndpoint;

public class PipelineManager {

	public static PlumberEndpoint[] connectMediaPipelines(
			MediaPipeline sourcePipeline, MediaPipeline sinkPipeline) {

		PlumberEndpoint sourcePipelinePlumber = new PlumberEndpoint.Builder(
				sourcePipeline).build();

		PlumberEndpoint sinkPipelinePlumber = new PlumberEndpoint.Builder(
				sourcePipeline).build();

		String sinkHost = sinkPipelinePlumber.getAddress();
		int port = sinkPipelinePlumber.getPort();

		sourcePipelinePlumber.link(sinkHost, port);

		return new PlumberEndpoint[] { sourcePipelinePlumber,
				sinkPipelinePlumber };
	}

}
