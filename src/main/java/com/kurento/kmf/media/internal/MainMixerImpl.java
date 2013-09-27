package com.kurento.kmf.media.internal;

import com.kurento.kmf.media.MainMixer;
import com.kurento.kmf.media.internal.refs.MediaMixerRefDTO;

@ProvidesMediaElement(type = MainMixerImpl.TYPE)
public class MainMixerImpl extends MediaMixerImpl implements MainMixer {

	public static final String TYPE = "MainMixer";

	MainMixerImpl(MediaMixerRefDTO mainMixerId) {
		super(mainMixerId);
	}

}
