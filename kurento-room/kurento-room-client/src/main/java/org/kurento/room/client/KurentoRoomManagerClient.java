package org.kurento.room.client;

import org.kurento.jsonrpc.client.JsonRcpServiceAdapter;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.client.JsonRpcClientWebSocket;
import org.kurento.room.client.internal.RoomClientService;

public class KurentoRoomManagerClient extends KurentoRoomClient {

	KurentoRoomManagerClient(RoomClientService service) {
		super(service);
	}

	public static KurentoRoomManagerClient create(String wsUri) {
		return create(new JsonRpcClientWebSocket(wsUri));
	}

	public static KurentoRoomManagerClient create(JsonRpcClient jsonRpcClient) {
		return new KurentoRoomManagerClient(
				JsonRcpServiceAdapter.createService(RoomClientService.class));
	}

	public static KurentoRoomManagerClient create(RoomClientService service) {
		return new KurentoRoomManagerClient(service);
	}

	public Room createRoom(String id) {
		return Room.create(service, id);
	}

}
