/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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
package com.kurento.kmf.media.mock;

import java.util.List;
import java.util.Random;

import org.apache.thrift.TException;

import com.kurento.kms.thrift.api.Command;
import com.kurento.kms.thrift.api.CommandResult;
import com.kurento.kms.thrift.api.MediaElementType;
import com.kurento.kms.thrift.api.MediaMixerType;
import com.kurento.kms.thrift.api.MediaObjectRef;
import com.kurento.kms.thrift.api.MediaObjectType;
import com.kurento.kms.thrift.api.MediaPadType;
import com.kurento.kms.thrift.api.MediaPipelineType;
import com.kurento.kms.thrift.api.MediaServerException;
import com.kurento.kms.thrift.api.MediaServerService;
import com.kurento.kms.thrift.api.MediaType;
import com.kurento.kms.thrift.api.PadDirection;

public class MediaServerMock implements MediaServerService.Iface {

	private final Random rnd = new Random();

	private enum ObjectType {
		ELEMENT, PAD, MIXER, PIPELINE
	}

	@Override
	public void connect(MediaObjectRef src, MediaObjectRef sink)
			throws MediaServerException, TException {
		// TODO Auto-generated method stub

	}

	@Override
	public MediaObjectRef createMediaElement(MediaObjectRef pipeline,
			String type) throws MediaServerException, TException {
		return generateObjectRef(ObjectType.ELEMENT);
	}

	@Override
	public MediaObjectRef createMediaElementWithParams(MediaObjectRef pipeline,
			String type, Command command) throws MediaServerException,
			TException {
		return generateObjectRef(ObjectType.ELEMENT);
	}

	@Override
	public MediaObjectRef createMediaMixer(MediaObjectRef pipeline, String type)
			throws MediaServerException, TException {
		return generateObjectRef(ObjectType.MIXER);
	}

	@Override
	public MediaObjectRef createMediaMixerWithParams(MediaObjectRef pipeline,
			String type, Command command) throws MediaServerException,
			TException {
		return generateObjectRef(ObjectType.MIXER);
	}

	@Override
	public MediaObjectRef createMediaPipeline() throws MediaServerException,
			TException {
		return generateObjectRef(ObjectType.PIPELINE);
	}

	@Override
	public MediaObjectRef createMediaPipelineWithParams(Command command)
			throws MediaServerException, TException {
		return generateObjectRef(ObjectType.PIPELINE);
	}

	@Override
	public MediaObjectRef createMixerEndPoint(MediaObjectRef mixer)
			throws MediaServerException, TException {
		return generateObjectRef(ObjectType.PAD);
	}

	@Override
	public MediaObjectRef createMixerEndPointWithParams(MediaObjectRef mixer,
			Command command) throws MediaServerException, TException {
		return generateObjectRef(ObjectType.PAD);
	}

	@Override
	public void disconnect(MediaObjectRef src, MediaObjectRef sink)
			throws MediaServerException, TException {
		// TODO Auto-generated method stub

	}

	@Override
	public List<MediaObjectRef> getConnectedSinks(MediaObjectRef src)
			throws MediaServerException, TException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MediaObjectRef getConnectedSrc(MediaObjectRef sink)
			throws MediaServerException, TException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MediaObjectRef getMediaElement(MediaObjectRef pipeline)
			throws MediaServerException, TException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MediaObjectRef getMediaPipeline(MediaObjectRef obj)
			throws MediaServerException, TException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<MediaObjectRef> getMediaSinks(MediaObjectRef element)
			throws MediaServerException, TException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<MediaObjectRef> getMediaSinksByFullDescription(
			MediaObjectRef element, MediaType type, String descr)
			throws MediaServerException, TException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<MediaObjectRef> getMediaSinksByMediaType(
			MediaObjectRef element, MediaType type)
			throws MediaServerException, TException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<MediaObjectRef> getMediaSrcs(MediaObjectRef element)
			throws MediaServerException, TException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<MediaObjectRef> getMediaSrcsByFullDescription(
			MediaObjectRef element, MediaType type, String descr)
			throws MediaServerException, TException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<MediaObjectRef> getMediaSrcsByMediaType(MediaObjectRef element,
			MediaType type) throws MediaServerException, TException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MediaObjectRef getParent(MediaObjectRef obj)
			throws MediaServerException, TException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getVersion() throws TException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void keepAlive(MediaObjectRef obj) throws MediaServerException,
			TException {
		// TODO Auto-generated method stub

	}

	@Override
	public void release(MediaObjectRef element) throws MediaServerException,
			TException {
		// TODO Auto-generated method stub

	}

	@Override
	public CommandResult sendCommand(MediaObjectRef element, Command command)
			throws MediaServerException, TException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String subscribe(MediaObjectRef element, String type,
			String address, int port) throws MediaServerException, TException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void unsubscribe(MediaObjectRef element, String token)
			throws MediaServerException, TException {
		// TODO Auto-generated method stub

	}

	private MediaObjectRef generateObjectRef(ObjectType type) {
		MediaObjectRef objRef = new MediaObjectRef();
		objRef.id = rnd.nextLong();
		objRef.token = Integer.toString(rnd.nextInt());
		objRef.type = new MediaObjectType();

		switch (type) {
		case ELEMENT:
			MediaElementType element = new MediaElementType("TODO");
			objRef.type.setElementType(element);
			break;
		case MIXER:
			MediaMixerType mixer = new MediaMixerType("TODO");
			objRef.type.setMixerType(mixer);
			break;
		case PAD:
			MediaPadType pad = new MediaPadType(PadDirection.SINK,
					MediaType.VIDEO, "TODO");
			objRef.type.setPadType(pad);
			break;
		case PIPELINE:
			MediaPipelineType pipeline = new MediaPipelineType();
			objRef.type.setPipelineType(pipeline);
			break;
		}

		return objRef;
	}

}
