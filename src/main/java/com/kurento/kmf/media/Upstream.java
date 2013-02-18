package com.kurento.kmf.media;

import java.io.Serializable;


// Es serializable, porque esta información debe poder almacenarse en la sesión SIP/HTTP
public interface Upstream extends Serializable {
	
	public void publish (MediaSource source);
	public MediaSource getSource ();

	public void unpublish();
	

}
