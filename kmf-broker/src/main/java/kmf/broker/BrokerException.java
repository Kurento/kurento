package kmf.broker;

public class BrokerException extends RuntimeException {

	private static final long serialVersionUID = 8339661146128257545L;

	public BrokerException(String message) {
		super(message);
	}

	public BrokerException(String message, Throwable cause) {
		super(message, cause);
	}
}
