package com.kurento.kmf.media;

import com.kurento.kmf.media.internal.refs.MediaElementRefDTO;

@IsMediaElement(type = ZBarFilter.TYPE)
public class ZBarFilter extends Filter {

	public static final String TYPE = "ZBarFilter";

	ZBarFilter(MediaElementRefDTO filterId) {
		super(filterId);
	}

}
