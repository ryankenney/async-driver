package info.ryankenney.jasync_driver;

import java.util.ArrayList;
import java.util.List;

/**
 * The central class of the jasync-driver library. See
 * https://github.com/ryankenney/jasync-driver/ for more information about
 * usage.
 * 
 * @author rkenney
 */
public class JasyncDriver {

	public JasyncDriver() {}
	
	private DriverBody body;
	private List<HistoryEntry> historyOfExecutedTasks = new ArrayList<>();
	int stepInLogicGraph;

	/**
	 * Executes the asynchronous/synchronous logic defined within the provided
	 * {@link DriverBody}. Note that this method will return as soon as the
	 * first asynchronous task within the {@link DriverBody} is started. If you
	 * need to respond to completion of the entire {@link DriverBody}, provide a
	 * callback to {@link #execute(DriverBody, Runnable)}.
	 * 
	 * @param driverBody
	 *            The asynchronous/synchronous logic to execute.
	 *            
	 * @throws UnstableConditionsException
	 *             If, on recursive executions of the {@link DriverBody} (during
	 *             asynchronous lulls) the execution path through
	 *             {@link DriverBody#run()} changes. This generally indicates
	 *             that {@link DriverBody#run()} is accessing non-local
	 *             variables that are changing, and the access to those
	 *             variables should be wrapped in {@link AsyncTask}/
	 *             {@link SyncTask} object(s).
	 */
	public void execute(DriverBody driverBody) throws UnstableConditionsException {
		execute(driverBody, null);
	}

	/**
	 * Executes the asynchronous/synchronous logic defined within the provided
	 * {@link DriverBody}. Executes the provided {@link Runnable} when the final
	 * {@link Task} is complete. Note that this method will return as soon as
	 * the first asynchronous task within the {@link DriverBody} is started,
	 * thus the need for the {@link Runnable}.
	 * 
	 * @param driverBody
	 *            The asynchronous/synchronous logic to execute.
	 * @param onComplete
	 *            The callback to execute when the final {@link Task} is
	 *            complete. If null, ignored.
	 *            
	 * @throws UnstableConditionsException
	 *             If, on recursive executions of the {@link DriverBody} (during
	 *             asynchronous lulls) the execution path through
	 *             {@link DriverBody#run()} changes. This generally indicates
	 *             that {@link DriverBody#run()} is accessing non-local
	 *             variables that are changing, and the access to those
	 *             variables should be wrapped in {@link AsyncTask}/
	 *             {@link SyncTask} object(s).
	 */
	public void execute(DriverBody driverBody, Runnable onComplete) throws UnstableConditionsException {
		this.body = driverBody;
		stepInLogicGraph = 0;
		try {
			driverBody.run();
			// Reset for possible reuse.
			body = null;
			historyOfExecutedTasks = new ArrayList<>();
			// The body completed. Execute any on-complete callback and return.
			if (onComplete != null) {
				onComplete.run();
			}
		} catch (JasyncActionSubmittedInterrupt a) {
			// OK. Suspend the logic until the async's callback wakes us back up.
		}
	}

	/**
	 * Executes the provided {@link AsyncTask}/{@link SyncTask}. This method
	 * should only be used within the body of a {@link DriverBody} that will be
	 * subsequently be {@link #execute(DriverBody)}'d by this object.</p>
	 * 
	 * More technically, this method only executes the provided {@link Task} if
	 * the {@link Task} has not yet been executed since the {@link DriverBody}
	 * was launched via {@link #execute(DriverBody)}. After that, each execution
	 * of this method returns the cached return value for the {@link Task}. This
	 * is part of the illusion that makes asynchronous actions look synchronous
	 * to the developer.</p>
	 * 
	 * @param task
	 *            The {@link AsyncTask}/{@link SyncTask} to execute
	 * @return The value returned by the task.
	 * 
	 * @throws UnstableConditionsException
	 *             If, on recursive executions of the {@link DriverBody} (during
	 *             asynchronous lulls) the execution path through
	 *             {@link DriverBody#run()} changes. This generally indicates
	 *             that {@link DriverBody#run()} is accessing non-local
	 *             variables that are changing, and the access to those
	 *             variables should be wrapped in {@link AsyncTask}/
	 *             {@link SyncTask} object(s).
	 */
	public <A,R> R execute(final Task<A, R> task) {
		return execute(task, null);
	}
	
	/**
	 * Executes the provided {@link AsyncTask}/{@link SyncTask} against the
	 * provided argument. This method should only be used within the body of a
	 * {@link DriverBody} that will be subsequently be
	 * {@link #execute(DriverBody)}'d by this object.</p>
	 * 
	 * More technically, this method only executes the provided {@link Task} if
	 * the {@link Task} has not yet been executed since the {@link DriverBody}
	 * was launched via {@link #execute(DriverBody)}. After that, each execution
	 * of this method returns the cached return value for the {@link Task}. This
	 * is part of the illusion that makes asynchronous actions look synchronous
	 * to the developer.</p>
	 * 
	 * @param task
	 *            The {@link AsyncTask}/{@link SyncTask} to execute
	 * @param arg
	 *            The argument to pass to the task.
	 * @return The value returned by the task.
	 * 
	 * @throws UnstableConditionsException
	 *             If, on recursive executions of the {@link DriverBody} (during
	 *             asynchronous lulls) the execution path through
	 *             {@link DriverBody#run()} changes. This generally indicates
	 *             that {@link DriverBody#run()} is accessing non-local
	 *             variables that are changing, and the access to those
	 *             variables should be wrapped in {@link AsyncTask}/
	 *             {@link SyncTask} object(s).
	 */
	public <A,R> R execute(final Task<A,R> task, final A arg) throws UnstableConditionsException {
		if (stepInLogicGraph < historyOfExecutedTasks.size()) {
			HistoryEntry previousTaskExecution = historyOfExecutedTasks.get(stepInLogicGraph);
			if (task != previousTaskExecution.task) {
				throw new UnstableConditionsException(String.format(
						"Task #%s in the execution path differs from the execution history", stepInLogicGraph+1));
			}
			stepInLogicGraph++;
			@SuppressWarnings("unchecked")
			R result = (R) previousTaskExecution.result;
			return result;
		}
		if (task instanceof AsyncTask) {
			AsyncTask<A,R> asyncTask = ((AsyncTask<A,R>) task);
			asyncTask.run(arg, new ResultHandler<R>() {
				@Override
				public void reportComplete(R result) {
					historyOfExecutedTasks.add(new HistoryEntry(task, result));
					execute(body);
				}
				@Override
				public void reportComplete() {
					historyOfExecutedTasks.add(new HistoryEntry(task, null));
					execute(body);
				}
			});
			throw new JasyncActionSubmittedInterrupt();
		} else {
			SyncTask<A,R> syncTask = ((SyncTask<A,R>) task);
			R result = syncTask.run(arg);
			stepInLogicGraph++;
			historyOfExecutedTasks.add(new HistoryEntry(task, result));
			return result;
		}
	}
	
	private static class HistoryEntry {
		private Task<?,?> task;
		private Object result;

		HistoryEntry(Task<?,?> task, Object result) {
			this.task = task;
			this.result = result;
		}
	}
}