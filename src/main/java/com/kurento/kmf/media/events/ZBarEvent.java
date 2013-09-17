package com.kurento.kmf.media.events;

import com.kurento.kms.thrift.api.MediaEvent;

public class ZBarEvent extends KmsEvent {

	public ZBarEvent(MediaEvent event) {
		super(event);
	}
	//
	// private String type;
	// private String value;
	//
	// public zZBarEvent(zMediaObject source, String type, String value) {
	// super(source);
	// this.type = type;
	// this.value = value;
	// }
	//
	// public String getType() {
	// return type;
	// }
	//
	// public String getValue() {
	// return value;
	// }
	//
	// @Override
	// public String toString() {
	// return this.getClass().getSimpleName() + "{\n" + "\t type: "
	// + getType() + ",\n" + "\t value: " + getValue() + "\n}\n";
	// }
}
