package org.kurento.room.server.app;

import java.util.List;

import org.kurento.room.client.event.Listener;
import org.kurento.room.client.event.StreamAddedEvent;
import org.kurento.room.client.internal.RoomClientService;
import org.kurento.room.client.internal.StreamInfo;

public class RoomClientServiceImpl implements RoomClientService {

	public RoomClientServiceImpl(RoomManager roomManager) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public String createRoom(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<StreamInfo> getRoomStreams(String token) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addEventListener(Class<StreamAddedEvent> class1,
			Listener listener) {
		// TODO Auto-generated method stub

	}

}
