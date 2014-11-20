package org.kurento.room.server.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;

import org.junit.Test;
import org.kurento.room.client.KurentoRoomClient;
import org.kurento.room.client.KurentoRoomManagerClient;
import org.kurento.room.client.Room;
import org.kurento.room.client.Stream;
import org.kurento.room.client.event.Listener;
import org.kurento.room.client.event.StreamAddedEvent;
import org.kurento.room.client.internal.RoomClientService;
import org.kurento.room.server.app.RoomClientServiceImpl;
import org.kurento.room.server.app.RoomManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultipleThreadsRoomTest {

	private static final Logger log = LoggerFactory
			.getLogger(MultipleThreadsRoomTest.class);

	private static final int NUM_PARTICIPANTS = 4;

	@Test
	public void test() throws Exception {

		RoomClientService service = new RoomClientServiceImpl(new RoomManager());

		final KurentoRoomManagerClient kurentoRoomManager = KurentoRoomManagerClient
				.create(service);

		final String token = kurentoRoomManager.createRoom("Room 1");

		ExecutorCompletionService<Room> exec = new ExecutorCompletionService<>(
				Executors.newFixedThreadPool(NUM_PARTICIPANTS));

		for (int i = 0; i < NUM_PARTICIPANTS; i++) {
			final KurentoRoomClient participantClient = KurentoRoomClient
					.create(service);
			final int userNum = i;
			exec.submit(new Callable<Room>() {
				@Override
				public Room call() throws Exception {
					return simulateUser(participantClient, token, userNum);
				}
			});
		}

		for (int i = 0; i < NUM_PARTICIPANTS; i++) {
			Room room = exec.take().get();

			log.info("Room: " + room);

			assertThat(room.getRemoteStreams().size(), is(NUM_PARTICIPANTS - 1));
		}
	}

	private Room simulateUser(KurentoRoomClient roomClient, String roomToken,
			int userNum) throws InterruptedException {

		Room room = roomClient.getRoom(roomToken);

		room.addEventListener(StreamAddedEvent.class,
				new Listener<StreamAddedEvent>() {
					@Override
					public void onEvent(StreamAddedEvent event) {
						log.info("Event: New remote stream with id: "
								+ event.getStream().getId());
					}
				});

		Map<String, Object> atts = new HashMap<>();
		atts.put("user", "user" + userNum);
		Stream localStream = new Stream(atts);

		Thread.sleep(1000);

		room.publish(localStream);

		Thread.sleep(3000);

		return room;
	}
}
