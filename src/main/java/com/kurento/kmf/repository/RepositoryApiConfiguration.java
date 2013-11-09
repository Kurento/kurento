package com.kurento.kmf.repository;

public class RepositoryApiConfiguration {

	private String webappPublicURL = "";

	/**
	 * Repository type: "filesystem" or "mongodb"
	 * 
	 * @return
	 */
	private String repositoryType = "filesystem";

	private String fileSystemFolder = "repository";

	private String mongoDatabaseName = "kmf-repository";

	private String mongoGridFSCollectionName = "fs";

	/**
	 * Connection URI as specified in
	 * http://docs.mongodb.org/manual/reference/connection-string/
	 * 
	 * @return
	 */

	private String mongoURLConnection = "mongodb://localhost";

	public String getWebappPublicURL() {
		return webappPublicURL;
	}

	public void setWebappPublicURL(String webappPublicURL) {
		this.webappPublicURL = webappPublicURL;
	}

	public String getRepositoryType() {
		return repositoryType;
	}

	public void setRepositoryType(String repositoryType) {
		this.repositoryType = repositoryType;
	}

	public String getFileSystemFolder() {
		return fileSystemFolder;
	}

	public void setFileSystemFolder(String fileSystemFolder) {
		this.fileSystemFolder = fileSystemFolder;
	}

	public String getMongoDatabaseName() {
		return mongoDatabaseName;
	}

	public void setMongoDatabaseName(String mongoDatabaseName) {
		this.mongoDatabaseName = mongoDatabaseName;
	}

	public String getMongoGridFSCollectionName() {
		return mongoGridFSCollectionName;
	}

	public void setMongoGridFSCollectionName(String mongoGridFSCollectionName) {
		this.mongoGridFSCollectionName = mongoGridFSCollectionName;
	}

	public String getMongoURLConnection() {
		return mongoURLConnection;
	}

	public void setMongoURLConnection(String mongoURLConnection) {
		this.mongoURLConnection = mongoURLConnection;
	}
}