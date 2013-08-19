package com.kurento.kmf.media;

public class PlayerEvent extends KmsEvent {

	public enum PlayerEventType {
		EOS,
	}

	private final PlayerEventType type;

	PlayerEvent(MediaObject source, PlayerEventType type) {
		super(source);
		this.type = type;
	}

	public PlayerEventType getType() {
		return type;
	}

}
