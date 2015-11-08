package info.ryankenney.jasync_driver;

/**
 * Used to define the asynchronous/synchronous logic managed by a
 * {@link JasyncDriver} instance.
 * 
 * @author rkenney
 */
public interface DriverBody {
	
	/**
	 * <p>
	 * Recursively executed by {@link JasyncDriver} as each contained
	 * {@link AsyncTask} triggers a callback. During recursive re-xecutions, the
	 * contained {@link AsyncTask}/{@link SyncTask}s return cached values, which
	 * results in perceived single execution. For this illusion to hold, it is
	 * critically important that the conditional logic within the
	 * {@link DriverBody} stay stable from recursive run to recursive run. The
	 * easiest way to do this is to obey two rules:
	 * 
	 * <ul>
	 * 
	 * <li>Wrap all reads/writes of non-local, non-mutable data in
	 * {@link AsyncTask}/ {@link SyncTask}. This includes any class member
	 * variables which could be read/written by other parts of the application
	 * while {@link JasyncDriver} awaits an asnychronous response.</li>
	 * 
	 * <li>Never use try/catch around {@link AsyncTask}/{@link SyncTask}
	 * executions, inside of {@link DriverBody#run()}. Instead, do any exception
	 * handling within the body of the {@link AsyncTask}/{@link SyncTask}
	 * objects.</li>
	 * 
	 * </ul>
	 * 
	 * <p>
	 * I highly recommend that a warning such as this be posted above each
	 * {@link DriverBody} implementation:
	 * </p>
	 * 
	 * <pre>
	 * // ATTENTION: Familiarize yourself with the rules of DriverBody before
	 * // editing this block. The DriverBody.body() is recursively executed,
	 * // with the result of Task executions read from cache.
	 * </pre>
	 * 
	 * <p>
	 * For more information about these caveats, see <a
	 * href="https://github.com/ryankenney/jasync-driver"
	 * >https://github.com/ryankenney/jasync-driver</a>.
	 * </p>
	 */
	public void run();

}
