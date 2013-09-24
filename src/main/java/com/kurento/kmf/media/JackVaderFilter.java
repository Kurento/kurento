package com.kurento.kmf.media;

import com.kurento.kmf.media.internal.refs.MediaElementRefDTO;

@IsMediaElement(type = JackVaderFilter.TYPE)
public class JackVaderFilter extends Filter {

	public static final String TYPE = "JackVaderFilter";

	JackVaderFilter(MediaElementRefDTO filterId) {
		super(filterId);
	}

}
