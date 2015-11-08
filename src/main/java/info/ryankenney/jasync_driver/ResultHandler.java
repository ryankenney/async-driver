package info.ryankenney.jasync_driver;

/**
 * A callback that handles the result of an asynchronous action. Asynchronous
 * actions should call one of the two provided methods to indicate completion.
 * 
 * @author rkenney
 *
 * @param <R>
 *            The type of data reported.
 */
public interface ResultHandler<R> {
	
	/**
	 * Called when an asynchronous action is complete, and a return is being
	 * reported.
	 * 
	 * @param result
	 *            The return value to report.
	 */
	void reportComplete(R result);

	/**
	 * Called when an asynchronous action is complete, but no return is being
	 * reported. Generally used when the implementation of has a return type of
	 * {@link Void}.
	 */
	void reportComplete();
}
