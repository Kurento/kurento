package org.kurento.room.client.internal;

import org.kurento.room.client.RoomInfo;

public interface RoomClientService {

	String createRoom(String id);

	String publish(String token, LocalStreamInfo localStreamInfo);

	void unpublish(String token, String id);

	String startRecording(String token, String id);

	void stopRecording(String token, String recordingId);

	void sendData(String id, String data);

	void removeRoomListener(String token);

	void setStreamListener(String token, String id,
			StreamListener streamListener);

	void removeStreamListener(String token, String id);

	RoomInfo createRoom(String id, RoomListener roomListener);

	RoomInfo getRoom(String token, RoomListener roomListener);

}
