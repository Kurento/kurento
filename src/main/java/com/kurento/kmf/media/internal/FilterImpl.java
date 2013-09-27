package com.kurento.kmf.media.internal;

import com.kurento.kmf.media.Filter;
import com.kurento.kmf.media.internal.refs.MediaElementRefDTO;

public class FilterImpl extends MediaElementImpl implements Filter {

	FilterImpl(MediaElementRefDTO filterId) {
		super(filterId);
	}

}
