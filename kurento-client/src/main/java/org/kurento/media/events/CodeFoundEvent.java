/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package org.kurento.media.events;

import org.kurento.media.MediaObject;
import org.kurento.media.ZBarFilter;
import org.kurento.tool.rom.server.Param;

/**
 * 
 * Event raised by a {@link ZBarFilter} when a code is found in the data being
 * streamed.
 * 
 **/
public class CodeFoundEvent extends MediaEvent {

	/**
	 * 
	 * type of <a
	 * href="http://www.kurento.org/docs/current/glossary.html#term-qr">QR</a>
	 * code found
	 * 
	 **/
	private String codeType;
	/**
	 * 
	 * value contained in the <a
	 * href="http://www.kurento.org/docs/current/glossary.html#term-qr">QR</a>
	 * code
	 * 
	 **/
	private String value;

	/**
	 * 
	 * Event raised by a {@link ZBarFilter} when a code is found in the data
	 * being streamed.
	 * 
	 * @param source
	 *            Object that raised the event
	 * @param type
	 *            Type of event that was raised
	 * @param codeType
	 *            type of <a href=
	 *            "http://www.kurento.org/docs/current/glossary.html#term-qr"
	 *            >QR</a> code found
	 * @param value
	 *            value contained in the <a href=
	 *            "http://www.kurento.org/docs/current/glossary.html#term-qr"
	 *            >QR</a> code
	 * 
	 **/
	public CodeFoundEvent(@Param("source") MediaObject source,
			@Param("type") String type, @Param("codeType") String codeType,
			@Param("value") String value) {
		super(source, type);
		this.codeType = codeType;
		this.value = value;
	}

	/**
	 * 
	 * Getter for the codeType property
	 * 
	 * @return type of <a
	 *         href="http://www.kurento.org/docs/current/glossary.html#term-qr"
	 *         >QR</a> code found *
	 **/
	public String getCodeType() {
		return codeType;
	}

	/**
	 * 
	 * Setter for the codeType property
	 * 
	 * @param codeType
	 *            type of <a href=
	 *            "http://www.kurento.org/docs/current/glossary.html#term-qr"
	 *            >QR</a> code found
	 * 
	 **/
	public void setCodeType(String codeType) {
		this.codeType = codeType;
	}

	/**
	 * 
	 * Getter for the value property
	 * 
	 * @return value contained in the <a
	 *         href="http://www.kurento.org/docs/current/glossary.html#term-qr"
	 *         >QR</a> code *
	 **/
	public String getValue() {
		return value;
	}

	/**
	 * 
	 * Setter for the value property
	 * 
	 * @param value
	 *            value contained in the <a href=
	 *            "http://www.kurento.org/docs/current/glossary.html#term-qr"
	 *            >QR</a> code
	 * 
	 **/
	public void setValue(String value) {
		this.value = value;
	}

}
