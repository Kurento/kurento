package org.kurento.room.client;

import org.kurento.jsonrpc.client.JsonRcpServiceAdapter;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.client.JsonRpcClientWebSocket;
import org.kurento.room.client.internal.RoomClientService;

public class KurentoRoomClient {

	protected RoomClientService service;

	KurentoRoomClient(RoomClientService service) {
		this.service = service;
	}

	public static KurentoRoomClient create(String wsUri) {
		return create(new JsonRpcClientWebSocket(wsUri));
	}

	public static KurentoRoomClient create(JsonRpcClient jsonRpcClient) {
		return new KurentoRoomClient(
				JsonRcpServiceAdapter.createService(RoomClientService.class));
	}

	public static KurentoRoomClient create(RoomClientService service) {
		return new KurentoRoomClient(service);
	}

	public Room getRoom(String token) {
		return Room.get(service, token);
	}
}
