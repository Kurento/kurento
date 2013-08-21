package com.kurento.kmf.media;

public class ZBarEvent extends KmsEvent {

	private String type;
	private String value;

	public ZBarEvent(MediaObject source, String type, String value) {
		super(source);
		this.type = type;
		this.value = value;
	}

	public String getType() {
		return type;
	}

	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "{\n" + "\t type: "
				+ getType() + ",\n" + "\t value: " + getValue() + "\n}\n";
	}
}
