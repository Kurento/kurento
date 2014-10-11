package org.kurento.tree.protocol;

public class TreeEndpoint {

	String sdp;
	String id;

	public TreeEndpoint(String sdp, String id) {
		super();
		this.sdp = sdp;
		this.id = id;
	}

	public String getSdp() {
		return sdp;
	}

	public String getId() {
		return id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((sdp == null) ? 0 : sdp.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TreeEndpoint other = (TreeEndpoint) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (sdp == null) {
			if (other.sdp != null)
				return false;
		} else if (!sdp.equals(other.sdp))
			return false;
		return true;
	}

}