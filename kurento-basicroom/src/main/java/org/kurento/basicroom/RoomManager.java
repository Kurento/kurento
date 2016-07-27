/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.kurento.basicroom;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PreDestroy;

import org.kurento.client.KurentoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Ivan Gracia (izanmail@gmail.com)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 1.0.0
 */
public class RoomManager {

  private final Logger log = LoggerFactory.getLogger(RoomManager.class);

  @Autowired
  private KurentoClient kurento;

  private final ConcurrentMap<String, Room> rooms = new ConcurrentHashMap<>();

  @PreDestroy
  public void close() {
    for (Room room : rooms.values()) {
      room.close();
    }
  }

  /**
   * Searches for a room by it's name.
   *
   * @param roomName
   *          the name of the room
   * @return the room if it was already created, or a new one if it is the first time this room is
   *         accessed
   */
  public Room getRoom(String roomName) {

    Room room = rooms.get(roomName);

    if (room == null) {

      room = new Room(roomName, kurento);
      Room oldRoom = rooms.putIfAbsent(roomName, room);
      if (oldRoom != null) {
        return oldRoom;
      } else {
        log.debug("Room {} not existent. Created new!", roomName);
        return room;
      }
    } else {
      return room;
    }
  }

  /**
   * Removes a room from the list of available rooms.
   *
   * @param room
   *          The room
   * @throws java.io.IOException
   *           If the room couldn't be closed
   */
  public void removeRoom(Room room) {
    this.rooms.remove(room.getName());
    room.close();
    log.debug("Room {} removed and closed", room.getName());
  }

}
