/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package org.kurento.kmf.media;

import org.kurento.tool.rom.RemoteClass;

/**
 * 
 * Base interface for all end points. An Endpoint is a {@link MediaElement} that
 * allow <a
 * href="http://www.kurento.org/docs/current/glossary.html#term-kms">KMS</a> to
 * interchange media contents with external systems, supporting different
 * transport protocols and mechanisms, such as <a
 * href="http://www.kurento.org/docs/current/glossary.html#term-rtp">RTP</a>, <a
 * href="http://<a href="http://www.kurento.org/docs/current/glossary.html#term-
 * http">HTTP</a>org/docs/current/glossary.html#term-webrtc">WebRTC</a>,
 * :term:`HTTP`, <code>file:/</code> URLs... An <code>Endpoint</code> may
 * contain both sources and sinks for different media types, to provide
 * bidirectional communication.
 * 
 **/
@RemoteClass
public interface Endpoint extends MediaElement {

}
