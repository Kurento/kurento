package com.kurento.kmf.media;

import com.kurento.kmf.media.internal.refs.MediaElementRefDTO;

@IsMediaElement(type = "ZBarFilter")
public class ZBarFilter extends Filter {

	ZBarFilter(MediaElementRefDTO filterId) {
		super(filterId);
	}

}
