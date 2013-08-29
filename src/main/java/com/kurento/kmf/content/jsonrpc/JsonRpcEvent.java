package com.kurento.kmf.content.jsonrpc;

/**
 * 
 * Java representation for JSON events.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
public class JsonRpcEvent {

	/**
	 * Event type.
	 */
	private String type;

	/**
	 * Event data.
	 */
	private String data;

	/**
	 * Static instance of JsonRpcEvent.
	 * 
	 * @param type
	 *            Event type
	 * @param data
	 *            Event data
	 * @return JsonRpcEvent instance
	 */
	public static JsonRpcEvent newEvent(String type, String data) {
		return new JsonRpcEvent(type, data);
	}

	/**
	 * Default constructor.
	 */
	JsonRpcEvent() {
	}

	/**
	 * Parameterized constructor.
	 * 
	 * @param type
	 *            Event type
	 * @param data
	 *            Event data
	 */
	JsonRpcEvent(String type, String data) {
		this.type = type;
		this.data = data;
	}

	/**
	 * Type accessor (getter).
	 * 
	 * @return event type
	 */
	public String getType() {
		return type;
	}

	/**
	 * Data accessor (getter).
	 * 
	 * @return event data
	 */
	public String getData() {
		return data;
	}
}
