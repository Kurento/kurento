package com.kurento.kmf.media.internal;

import com.kurento.kmf.media.ZBarFilter;
import com.kurento.kmf.media.internal.refs.MediaElementRefDTO;
import com.kurento.kms.thrift.api.mediaServerConstants;

@ProvidesMediaElement(type = mediaServerConstants.ZBAR_FILTER_TYPE)
public class ZBarFilterImpl extends FilterImpl implements ZBarFilter {

	ZBarFilterImpl(MediaElementRefDTO filterId) {
		super(filterId);
	}
}
