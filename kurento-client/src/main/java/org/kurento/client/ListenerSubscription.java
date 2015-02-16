/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package org.kurento.client;

/**
 * Interface to be implemented by objects that represent the subscription to an
 * event in Kurento. Implementers of this interface may be used by the system to
 * track listeners of events registered by users. Subscribing to a certain
 * {@link MediaEvent} raised by a {@link MediaObject} generates a
 * {@code ListenerSubscription}, that can be used by the client to unregister
 * this listener.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 2.0.0
 */
public interface ListenerSubscription {

	/**
	 * Returns the registration id for this listener
	 * 
	 * @return The id
	 */
	String getSubscriptionId();

}
