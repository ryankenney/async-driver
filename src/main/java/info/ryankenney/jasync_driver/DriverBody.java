package info.ryankenney.jasync_driver;

/**
 * Used to define the asynchronous/synchronous logic managed by a
 * {@link JasyncDriver} instance.
 * 
 * @author rkenney
 */
public interface DriverBody {
	
	/**
	 * Recursively executed by {@link JasyncDriver} as each contained
	 * {@link AsyncTask} triggers a callback. When implementing this method, it
	 * is critically important to follow two rules:</p>
	 * 
	 * <ul>
	 * <li>Wrap all reads/writes of non-local, non-mutable data in
	 * {@link AsyncTask}/ {@link SyncTask}</li>
	 * <li>Never use try/catch around {@link AsyncTask}/{@link SyncTask}
	 * executions. Instead, do any exeption handling within the body of the
	 * {@link AsyncTask}/{@link SyncTask}.</li>
	 * </ul>
	 * 
	 * For more information about these caveats, see
	 * https://github.com/ryankenney/jasync-driver.</p>
	 */
	public void run();

}
