/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */

package org.kurento.repository;

public class RepositoryApiConfiguration {

	private String webappPublicURL = "";
	private String repositoryType = "filesystem";
	private String fileSystemFolder = "repository";
	private String mongoDatabaseName = "kmf-repository";
	private String mongoGridFSCollectionName = "fs";
	private String mongoURLConnection = "mongodb://localhost";

	/**
	 * Returns the repository type. The default value is "filesystem" and can be
	 * changed with a properties file or with the method
	 * {@link RepositoryApiConfiguration#setRepositoryType(String)}.
	 * 
	 * @return the repository type.
	 */
	public String getWebappPublicURL() {
		return webappPublicURL;
	}

	/**
	 * Sets the type of the repository. The value can be "filesystem" or
	 * "mongodb".
	 * 
	 * @param webappPublicURL
	 *            URL for the web application
	 */
	public void setWebappPublicURL(String webappPublicURL) {
		this.webappPublicURL = webappPublicURL;
	}

	/**
	 * Returns the repository type. The default value is "filesystem" and can be
	 * changed with a properties file or with the method
	 * {@link RepositoryApiConfiguration#setRepositoryType(String)}.
	 * 
	 * @return the repository type.
	 */
	public String getRepositoryType() {
		return repositoryType;
	}

	/**
	 * Sets the type of the repository. The value can be "filesystem" or
	 * "mongodb".
	 * 
	 * @param repositoryType
	 *            type of the repository
	 */
	public void setRepositoryType(String repositoryType) {
		this.repositoryType = repositoryType;
	}

	/**
	 * Returns the folder path of the repository. The default value is
	 * "repository" and can be changed with a properties file or with the method
	 * {@link RepositoryApiConfiguration#setFileSystemFolder(String)}. This
	 * property is only used when the repository type is "filesystem".
	 * 
	 * @return the folder path of the repository.
	 */
	public String getFileSystemFolder() {
		return fileSystemFolder;
	}

	/**
	 * Sets the folder path of the repository. This property is only used when
	 * the repository type is "filesystem".
	 * 
	 * @param fileSystemFolder
	 *            folder, in the filesystem, that the repository will use to
	 *            save the files
	 */
	public void setFileSystemFolder(String fileSystemFolder) {
		this.fileSystemFolder = fileSystemFolder;
	}

	/**
	 * Returns the database name used for the repository. The default value is
	 * "kmf-repository" and can be changed with a properties file or with the
	 * method {@link RepositoryApiConfiguration#setMongoDatabaseName(String)}.
	 * This property is only used when the repository type is "mongodb".
	 * 
	 * @return the database name.
	 */
	public String getMongoDatabaseName() {
		return mongoDatabaseName;
	}

	/**
	 * Sets the database name used for the repository. This property is only
	 * used when the repository type is "mongodb".
	 * 
	 * @param mongoDatabaseName
	 *            The database name
	 */
	public void setMongoDatabaseName(String mongoDatabaseName) {
		this.mongoDatabaseName = mongoDatabaseName;
	}

	/**
	 * Returns the name of the gridfs collection used for the repository. The
	 * default value is "fs" and can be changed with a properties file or with
	 * the method
	 * {@link RepositoryApiConfiguration#setMongoGridFSCollectionName(String)}.
	 * This property is only used when the repository type is "mongodb".
	 * 
	 * @return the name of the gridfs collection.
	 */
	public String getMongoGridFSCollectionName() {
		return mongoGridFSCollectionName;
	}

	/**
	 * Sets the name of the gridfs collection used for the repository. This
	 * property is only used when the repository type is "mongodb".
	 * 
	 * @param mongoGridFSCollectionName
	 *            the name of the gridfs collection
	 */
	public void setMongoGridFSCollectionName(String mongoGridFSCollectionName) {
		this.mongoGridFSCollectionName = mongoGridFSCollectionName;
	}

	/**
	 * Returns the connection to mongo database. The default value is
	 * "mongodb://localhost" and can be changed with a properties file or with
	 * the method
	 * {@link RepositoryApiConfiguration#setMongoURLConnection(String)}. This
	 * property is only used when the repository type is "mongodb".
	 * 
	 * @return the connection to mongo database.
	 */
	public String getMongoURLConnection() {
		return mongoURLConnection;
	}

	/**
	 * Sets the connection to mongo database in the format specified in
	 * http://docs.mongodb.org/manual/reference/connection-string/. This
	 * property is only used when the repository type is "mongodb".
	 * 
	 * @param mongoURLConnection
	 *            connection URL for the mong database
	 */
	public void setMongoURLConnection(String mongoURLConnection) {
		this.mongoURLConnection = mongoURLConnection;
	}
}