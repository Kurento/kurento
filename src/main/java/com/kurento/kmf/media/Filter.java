package com.kurento.kmf.media;

import com.kurento.kmf.media.internal.refs.MediaElementRefDTO;
import com.kurento.kmf.media.objects.MediaElement;

@IsMediaElement(type = "Filter")
public abstract class Filter extends MediaElement {

	Filter(MediaElementRefDTO filterId) {
		super(filterId);
	}

}
