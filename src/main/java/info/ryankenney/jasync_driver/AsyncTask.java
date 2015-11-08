package info.ryankenney.jasync_driver;

/**
 * Used to define an asynchronous task (a task that uses a callback to indicate
 * completion, as opposed to a regular method return) managed by a
 * {@link JasyncDriver}.
 * 
 * @author rkenney
 *
 * @param <A>
 *            The type of argument passed into this task. Use the {@link Void}
 *            type if you have no use for an argument.
 * @param <R>
 *            The type returned by the task. Use the {@link Void} type if you
 *            have no use for an argument.
 */
public interface AsyncTask<A,R> extends Task<A,R> {

	/**
	 * The method that executes the underlying asynchronous action. It is
	 * critical that the implementation call
	 * {@link ResultHandler#reportComplete(Object)} or
	 * {@link ResultHandler#reportComplete()} when the action is complete.
	 * 
	 * @param arg
	 *            Any argument passed into the task. Arguments are controlled by
	 *            the {@link DriverBody#run()} definition(s) in which instances
	 *            of this class is used.
	 * 
	 * @param resultHandler
	 *            A callback used to indicate that this task is complete and
	 *            (optionally) report any return value.
	 */
	void run(A arg, ResultHandler<R> resultHandler);
	
}
