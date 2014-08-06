/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package org.kurento.media.events;

import org.kurento.media.MediaObject;
import org.kurento.tool.rom.server.Param;

/**
 * 
 * An error related to the MediaObject has occurred
 * 
 **/
public class ErrorEvent implements Event {

	/**
	 * 
	 * {@link MediaObject} where the error originated
	 * 
	 **/
	private MediaObject object;
	/**
	 * 
	 * Textual description of the error
	 * 
	 **/
	private String description;
	/**
	 * 
	 * Server side integer error code
	 * 
	 **/
	private int errorCode;
	/**
	 * 
	 * Integer code as a String
	 * 
	 **/
	private String type;

	/**
	 * 
	 * An error related to the MediaObject has occurred
	 * 
	 * @param object
	 *            {@link MediaObject} where the error originated
	 * @param description
	 *            Textual description of the error
	 * @param errorCode
	 *            Server side integer error code
	 * @param type
	 *            Integer code as a String
	 * 
	 **/
	public ErrorEvent(@Param("object") MediaObject object,
			@Param("description") String description,
			@Param("errorCode") int errorCode, @Param("type") String type) {
		super();
		this.object = object;
		this.description = description;
		this.errorCode = errorCode;
		this.type = type;
	}

	/**
	 * 
	 * Getter for the object property
	 * 
	 * @return {@link MediaObject} where the error originated *
	 **/
	public MediaObject getObject() {
		return object;
	}

	/**
	 * 
	 * Setter for the object property
	 * 
	 * @param object
	 *            {@link MediaObject} where the error originated
	 * 
	 **/
	public void setObject(MediaObject object) {
		this.object = object;
	}

	/**
	 * 
	 * Getter for the description property
	 * 
	 * @return Textual description of the error *
	 **/
	public String getDescription() {
		return description;
	}

	/**
	 * 
	 * Setter for the description property
	 * 
	 * @param description
	 *            Textual description of the error
	 * 
	 **/
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * 
	 * Getter for the errorCode property
	 * 
	 * @return Server side integer error code *
	 **/
	public int getErrorCode() {
		return errorCode;
	}

	/**
	 * 
	 * Setter for the errorCode property
	 * 
	 * @param errorCode
	 *            Server side integer error code
	 * 
	 **/
	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	/**
	 * 
	 * Getter for the type property
	 * 
	 * @return Integer code as a String *
	 **/
	public String getType() {
		return type;
	}

	/**
	 * 
	 * Setter for the type property
	 * 
	 * @param type
	 *            Integer code as a String
	 * 
	 **/
	public void setType(String type) {
		this.type = type;
	}

}
