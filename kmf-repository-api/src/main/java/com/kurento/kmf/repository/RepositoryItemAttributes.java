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

package com.kurento.kmf.repository;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Attributes implementation.
 */
public class RepositoryItemAttributes {

	/**
	 * HTTP date format.
	 */
	protected static final SimpleDateFormat format = new SimpleDateFormat(
			"EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);

	/**
	 * Date formats using for Date parsing.
	 */
	protected static final SimpleDateFormat formats[] = {
			new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US),
			new SimpleDateFormat("EEEEEE, dd-MMM-yy HH:mm:ss zzz", Locale.US),
			new SimpleDateFormat("EEE MMMM d HH:mm:ss yyyy", Locale.US) };

	protected static final TimeZone gmtZone = TimeZone.getTimeZone("GMT");

	/**
	 * GMT timezone - all HTTP dates are on GMT
	 */
	static {

		format.setTimeZone(gmtZone);

		formats[0].setTimeZone(gmtZone);
		formats[1].setTimeZone(gmtZone);
		formats[2].setTimeZone(gmtZone);

	}

	/**
	 * Content length.
	 */
	protected long contentLength = -1;

	/**
	 * Creation time.
	 */
	protected long creation = -1;

	/**
	 * Creation date.
	 */
	protected Date creationDate;

	/**
	 * Last modified time.
	 */
	protected long lastModified = -1;

	/**
	 * Last modified date.
	 */
	protected Date lastModifiedDate;

	/**
	 * Last modified date in HTTP format.
	 */
	protected String lastModifiedHttp;

	/**
	 * MIME type.
	 */
	protected String mimeType;

	/**
	 * Weak ETag.
	 */
	protected String weakETag;

	/**
	 * Strong ETag.
	 */
	protected String strongETag;

	/**
	 * Get content length.
	 * 
	 * @return content length value
	 */
	public long getContentLength() {
		return contentLength;
	}

	/**
	 * Set content length.
	 * 
	 * @param contentLength
	 *            New content length value
	 */
	public void setContentLength(long contentLength) {
		this.contentLength = contentLength;
	}

	/**
	 * Get creation time.
	 * 
	 * @return creation time value
	 */
	public long getCreation() {
		return creation;
	}

	/**
	 * Get creation date.
	 * 
	 * @return Creation date value
	 */
	public Date getCreationDate() {
		return creationDate;
	}

	/**
	 * Creation date mutator.
	 * 
	 * @param creationDate
	 *            New creation date
	 */
	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
		this.creation = creationDate.getTime();
	}

	/**
	 * Creation date mutator.
	 * 
	 * @param creation
	 *            New creation date as a long
	 */
	public void setCreation(long creation) {
		this.creation = creation;
		this.creationDate = new Date(creation);
	}

	public void setCreationDateHttp(String creationDateValue)
			throws ParseException {
		this.creationDate = parseHttpDate(creationDateValue);
		this.creation = creationDate.getTime();
	}

	/**
	 * Get last modified time.
	 * 
	 * @return lastModified time value
	 */
	public long getLastModified() {
		return lastModified;
	}

	/**
	 * Set last modified.
	 * 
	 * @param lastModified
	 *            New last modified value
	 */
	public void setLastModified(long lastModified) {
		this.lastModified = lastModified;
		this.lastModifiedDate = new Date(lastModified);
	}

	/**
	 * Get lastModified date.
	 * 
	 * @return LastModified date value
	 */
	public Date getLastModifiedDate() {
		return lastModifiedDate;
	}

	/**
	 * Last modified date mutator.
	 * 
	 * @param lastModifiedDate
	 *            New last modified date
	 */
	public void setLastModifiedDate(Date lastModifiedDate) {
		this.lastModified = lastModifiedDate.getTime();
		this.lastModifiedDate = lastModifiedDate;
	}

	/**
	 * @return Returns the lastModifiedHttp.
	 */
	public String getLastModifiedHttp() {

		if (lastModifiedHttp != null) {
			return lastModifiedHttp;
		}

		Date modifiedDate = getLastModifiedDate();
		if (modifiedDate == null) {
			modifiedDate = getCreationDate();
		}

		if (modifiedDate == null) {
			modifiedDate = new Date();
		}

		synchronized (format) {
			lastModifiedHttp = format.format(modifiedDate);
		}

		return lastModifiedHttp;
	}

	private Date parseHttpDate(String creationDate) throws ParseException {

		Date newCreationDate = null;

		// Parsing the HTTP Date
		for (int i = 0; (newCreationDate == null) && (i < formats.length); i++) {
			try {
				newCreationDate = formats[i].parse(creationDate);
			} catch (ParseException e) {
				// Ignore
			}
		}

		if (newCreationDate == null) {
			throw new ParseException("Exception while parsing http date", -1);
		} else {
			return newCreationDate;
		}
	}

	/**
	 * @param lastModifiedHttp
	 *            The lastModifiedHttp to set.
	 * @throws ParseException
	 */
	public void setLastModifiedHttp(String lastModifiedHttp)
			throws ParseException {
		this.lastModifiedDate = parseHttpDate(lastModifiedHttp);
		this.lastModified = lastModifiedDate.getTime();
		this.lastModifiedHttp = lastModifiedHttp;
	}

	/**
	 * @return Returns the mimeType.
	 */
	public String getMimeType() {
		return mimeType;
	}

	/**
	 * @param mimeType
	 *            The mimeType to set.
	 */
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	/**
	 * Get ETag.
	 * 
	 * @return strong ETag if available, else weak ETag.
	 */
	public String getETag() {

		String result = null;

		if (strongETag != null) {
			// The strong ETag must always be calculated by the resources
			result = strongETag;
		} else {
			// The weakETag is contentLength + lastModified
			if (weakETag == null) {
				long contentLength = getContentLength();
				long lastModified = getLastModified();
				if ((contentLength >= 0) || (lastModified >= 0)) {
					weakETag = "W/\"" + contentLength + "-" + lastModified
							+ "\"";
				}
			}
			result = weakETag;
		}

		return result;
	}

	/**
	 * Set strong ETag.
	 */
	public void setETag(String eTag) {
		this.strongETag = eTag;
	}

}
