package org.kurento.client;

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

}
