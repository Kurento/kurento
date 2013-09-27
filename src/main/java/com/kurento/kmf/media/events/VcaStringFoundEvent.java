package com.kurento.kmf.media.events;

public interface VcaStringFoundEvent extends MediaEvent {
	String getValueType();

	String getValue();
}
