package com.kurento.kmf.media;

import com.kurento.tool.rom.RemoteClass;
import com.kurento.tool.rom.server.Param;

@RemoteClass
public interface HubPort extends MediaElement {

	public interface Factory {

		public Builder create(@Param("hub") Hub hub);
	}

	public interface Builder extends AbstractBuilder<HubPort> {

		public Builder withGarbagePeriod(int garbagePeriod);
	}
}
