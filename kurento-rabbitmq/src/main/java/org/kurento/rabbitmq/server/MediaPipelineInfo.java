package org.kurento.rabbitmq.server;

public class MediaPipelineInfo {

	private String brokerPipelineId;
	private String realPipelineId;
	private String eventsExchange;

	public MediaPipelineInfo(String brokerPipelineId, String realPipelineId,
			String eventsExchange) {
		this.brokerPipelineId = brokerPipelineId;
		this.realPipelineId = realPipelineId;
		this.eventsExchange = eventsExchange;
	}

	public String getBrokerPipelineId() {
		return brokerPipelineId;
	}

	public String getRealPipelineId() {
		return realPipelineId;
	}

	public String getEventsExchange() {
		return eventsExchange;
	}

}
