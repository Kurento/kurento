package com.kurento.kmf.media.internal;

import com.kurento.kmf.media.JackVaderFilter;
import com.kurento.kmf.media.internal.refs.MediaElementRefDTO;
import com.kurento.kms.thrift.api.mediaServerConstants;

@ProvidesMediaElement(type = mediaServerConstants.JACK_VADER_FILTER_TYPE)
public class JackVaderFilterImpl extends FilterImpl implements JackVaderFilter {

	JackVaderFilterImpl(MediaElementRefDTO filterId) {
		super(filterId);
	}

}
