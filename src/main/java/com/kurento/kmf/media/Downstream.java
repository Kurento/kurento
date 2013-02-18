package com.kurento.kmf.media;

import java.util.List;
import java.io.Serializable;

// Es serializable, porque esta información debe poder almacenarse en la sesión SIP/HTTP
public interface Downstream extends Serializable {
	
	public void addPlayer(MediaPlayer remoteDisplay);
	public void removePlayer (MediaPlayer remoteDisplay);
	public List<MediaPlayer> getPlayers ();
	
	public void stop ();

}
