/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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
package com.kurento.demo.cpbrazil.youtube;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.java6.auth.oauth2.FileCredentialStore;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;

/**
 * Authorization for uploading videos to YouTube.
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 1.0.1
 * 
 */
class Auth {
	/**
	 * Global instance of the HTTP transport.
	 */
	public static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

	/**
	 * Global instance of the JSON factory.
	 */
	public static final JsonFactory JSON_FACTORY = new JacksonFactory();

	/**
	 * This is the directory that will be used under the user's home directory
	 * where OAuth tokens will be stored.
	 */
	private static final String CREDENTIALS_DIRECTORY = ".oauth-credentials";

	/**
	 * Authorises the installed application to access user's protected data.
	 * 
	 * @param scopes
	 *            list of scopes needed to run youtube upload.
	 * @param credentialDatastore
	 * @return
	 * @throws IOException
	 */
	public static Credential authorise(List<String> scopes,
			String credentialDatastore) throws IOException {

		// Set up file credential store.
		File file = new File(System.getProperty("user.home"),
				CREDENTIALS_DIRECTORY + "/" + credentialDatastore);
		return authorise(scopes, file);
	}

	public static Credential authorise(List<String> scopes,
			File credentialDataStore) throws IOException {

		FileCredentialStore credentialStore = new FileCredentialStore(
				credentialDataStore, JSON_FACTORY);

		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
				JSON_FACTORY,
				Auth.class.getResourceAsStream("/client_secrets.json"));

		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
				HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, scopes)
				.setCredentialStore(credentialStore).build();

		// TODO change this to remove the port
		LocalServerReceiver localReceiver = new LocalServerReceiver.Builder()
				.setPort(5555).build();

		// Authorize.
		return new AuthorizationCodeInstalledApp(flow, localReceiver)
				.authorize("user");
	}

	public static File inputStreamToFile(InputStream inputStream)
			throws IOException {
		File file = new File("credentialStore");
		OutputStream output = new FileOutputStream(file);
		byte[] buf = new byte[1024];
		int len;
		while ((len = inputStream.read(buf)) > 0) {
			output.write(buf, 0, len);
		}
		output.close();
		inputStream.close();
		return file;
	}

}
