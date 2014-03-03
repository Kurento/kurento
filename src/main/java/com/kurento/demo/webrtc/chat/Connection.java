/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package com.kurento.demo.webrtc.chat;

import java.util.ArrayList;
import java.util.List;

import com.kurento.kmf.content.ContentEvent;
import com.kurento.kmf.content.ContentSession;
import com.kurento.kmf.media.WebRtcEndpoint;

/**
 * Connection in the video conference chat room.
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 3.0.7
 */
public class Connection {

	private Transmitter transmitter;

	private List<Receiver> receivers;

	private int receiversSize;

	public Connection(int chatSize) {
		// The number of receivers is the chat size less one (the transmitter)
		receiversSize = chatSize - 1;
		receivers = new ArrayList<Receiver>(receiversSize);
	}

	public boolean allElementsPresent() {
		return allReceiversPresent() && transmitter != null;
	}

	public boolean allReceiversPresent() {

		return receivers.size() == receiversSize;
	}

	public boolean addReceiver(WebRtcEndpoint receiver,
			ContentSession contentSession) {
		if (allReceiversPresent()) {
			return false;
		}
		receivers.add(new Receiver(receiver, contentSession));
		return true;
	}

	public void connectTransmitter(List<Receiver> receivers) {
		for (Receiver receiver : receivers) {
			if (!receiver.isConnected()) {
				transmitter.getWebRtcEndpoint().connect(
						receiver.getWebRtcEndpoint());
				receiver.getContentSession().publishEvent(
						new ContentEvent("nickname", transmitter.getNick()));
				receiver.setConnected(true);
				break;
			}
		}
	}

	public void connectReceivers(Transmitter transmitter) {
		for (Receiver receiver : receivers) {
			if (!receiver.isConnected()) {
				transmitter.getWebRtcEndpoint().connect(
						receiver.getWebRtcEndpoint());
				receiver.getContentSession().publishEvent(
						new ContentEvent("nickname", transmitter.getNick()));
				receiver.setConnected(true);
				break;
			}
		}
	}

	public Transmitter getTransmitter() {
		return transmitter;
	}

	public void setTransmitter(WebRtcEndpoint webRtcEndpoint, String nick) {
		this.transmitter = new Transmitter(webRtcEndpoint, nick);
	}

	public List<Receiver> getReceivers() {
		return receivers;
	}

}
