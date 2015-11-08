package info.ryankenney.jasync_driver;

/**
 * Used to define a synchronous task managed by a {@link JasyncDriver}. At first
 * it may not seem necessary to use {@link SyncTask} objects, since in contrast
 * to asynchronous tasks, these need no special handling to operate
 * synchronously. However, this is very misleading. See {@link JasyncDriver} for
 * more information.
 * 
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
public interface SyncTask<A,R> extends Task<A,R> {

	/**
	 * The method that executes the underlying synchronous action.
	 * 
	 * @param arg
	 *            Any argument passed into the task. Arguments are controlled by
	 *            the {@link DriverBody#run()} definition(s) in which instances
	 *            of this class is used.
	 * @return Any value resulting from the task. Simply null if the return type
	 *         is {@link Void}.
	 */
	R run(A arg);
	
}
