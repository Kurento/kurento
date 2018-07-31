package org.kurento.client;

import org.kurento.jsonrpc.Props;

public interface GenericMediaElement extends MediaElement {

	public class Builder extends AbstractBuilder<GenericMediaElement> {

		public Builder(org.kurento.client.MediaPipeline mediaPipeline, String mediaElementClassName) {
			super(GenericMediaElement.class, mediaPipeline);
			props.add("mediaPipeline", mediaPipeline);
			props.add("mediaElementClassName", mediaElementClassName);
		}

		public Builder withConstructorParam(String name, Object value) {
			props.add(name, value);
			return this;
		}

		public Builder withProperties(Properties properties) {
			return (Builder) super.withProperties(properties);
		}

		public Builder with(String name, Object value) {
			return (Builder) super.with(name, value);
		}
	}

	public Object invoke(String method, Props params);

	public ListenerSubscription addEventListener(String type, EventListener<GenericMediaEvent> listener);

	public void removeEventListener(ListenerSubscription listener);
}
