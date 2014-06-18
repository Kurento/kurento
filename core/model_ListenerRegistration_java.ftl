${config.subfolder}/ListenerRegistration.java
/**
 * This file is generated with Kurento ktool-rom-processor.
 * Please don't edit. Changes should go to kms-interface-rom and
 * ktool-rom-processor templates.
 */
package ${config.packageName};

/**
 * Interface to be implemented by objects that represent the registration of a
 * listener in the system. Implementers of this interface may be used by the
 * system to track listeners of events registered by users. Subscribing to a
 * certain {@link MediaEvent} raised by a {@link MediaElement} generates a
 * {@code ListenerRegistration}, that can be used by the client to unregister
 * this listener.
 *
 * @author Luis López (llopez@gsyc.es)
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 2.0.0
 */
public interface ListenerRegistration {

	/**
	 * Returns the registration id for this listener
	 *
	 * @return The id
	 */
	String getRegistrationId();

}
