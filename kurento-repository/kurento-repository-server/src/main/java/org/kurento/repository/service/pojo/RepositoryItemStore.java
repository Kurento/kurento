package org.kurento.repository.service.pojo;

public class RepositoryItemStore {
	private String id;
	private String url;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	@Override
	public String toString() {
		return "[id=" + id + ", url=" + url + "]";
	}
}
