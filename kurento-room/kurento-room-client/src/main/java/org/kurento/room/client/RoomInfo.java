package org.kurento.room.client;

import java.util.List;

import org.kurento.room.client.internal.StreamInfo;

public class RoomInfo {

	private String token;
	private String id;
	private List<StreamInfo> remoteStreams;

	public RoomInfo() {
	}

	public RoomInfo(String token, String id, List<StreamInfo> remoteStreams) {
		this.token = token;
		this.id = id;
		this.remoteStreams = remoteStreams;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<StreamInfo> getRemoteStreams() {
		return remoteStreams;
	}

	public void setRemoteStreams(List<StreamInfo> remoteStreams) {
		this.remoteStreams = remoteStreams;
	}
}
