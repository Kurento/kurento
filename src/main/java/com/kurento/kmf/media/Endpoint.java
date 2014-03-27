/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package com.kurento.kmf.media;

import com.kurento.tool.rom.RemoteClass;

/**
 * 
 * Base interface for all end points. An Endpoint is a {@link MediaElement} that
 * allow <a
 * href="http://www.kurento.org/docs/current/glossary.html#term-KMS">KMS</a> to
 * interchange media contents with external systems, supporting different
 * transport protocols and mechanisms, such as <a
 * href="http://www.kurento.org/docs/current/glossary.html#term-RTP">RTP</a>, <a
 * href
 * ="http://www.kurento.org/docs/current/glossary.html#term-WebRTC">WebRTC</a>,
 * <a
 * href="http://www.kurento.org/docs/current/glossary.html#term-HTTP">HTTP</a>,
 * <code>file:/</code> URLs... An <code>Endpoint</code> may contain both sources
 * and sinks for different media types, to provide bidirectional communication.
 * 
 **/
@RemoteClass
public interface Endpoint extends MediaElement {

}
