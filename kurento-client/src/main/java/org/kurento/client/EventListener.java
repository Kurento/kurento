/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package org.kurento.client;

/**
 * 
 * Interface to be implemented by {@link MediaEvent} listeners. Implementors of
 * this interface will be on charge of processing the events raised by media
 * elements.
 * 
 * @param <T>
 *            A class that extends from {@link Event}
 * 
 * @author Luis López (llopez@gsyc.es), Ivan Gracia (igracia@kurento.org)
 * 
 **/
public interface EventListener<T extends Event> {
	/**
	 * 
	 * Called from the framework when an event is raised at the media server
	 * 
	 * @param event
	 *            a T event
	 * 
	 */
	void onEvent(T event);
}
