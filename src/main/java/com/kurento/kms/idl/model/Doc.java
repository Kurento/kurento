package com.kurento.kms.idl.model;

public class Doc extends ModelElement {

	private String doc;

	public Doc(String doc) {
		super();
		this.doc = doc;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Doc other = (Doc) obj;
		if (doc == null) {
			if (other.doc != null)
				return false;
		} else if (!doc.equals(other.doc))
			return false;
		return true;
	}

	public String getDoc() {
		return doc;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((doc == null) ? 0 : doc.hashCode());
		return result;
	}

	public void setDoc(String doc) {
		this.doc = doc;
	}

	@Override
	public String toString() {
		return doc;
	}

}
