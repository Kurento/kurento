package com.kurento.kmf.media;

import com.kurento.kms.api.MediaObject;
import com.kurento.kms.api.MixerType;

public class MainMixer extends Mixer {

	private static final long serialVersionUID = 1L;

	static final MixerType mixerType = MixerType.MAIN_MIXER;

	MainMixer(MediaObject mainMixer) {
		super(mainMixer);
	}

}
