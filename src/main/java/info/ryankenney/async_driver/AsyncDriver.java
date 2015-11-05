package info.ryankenney.async_driver;

import java.util.ArrayList;
import java.util.List;

public class AsyncDriver {

	public AsyncDriver() {}
	
	private DriverBody body;
	private List<HistoryEntry> historyOfExecutedTasks = new ArrayList<>();
	int stepInLogicGraph;
	
	public void execute(DriverBody driverBody) {
		this.body = driverBody;
		stepInLogicGraph = 0;
		try {
			driverBody.run();
			// Reset for possible reuse.
			body = null;
			historyOfExecutedTasks = new ArrayList<>();
			// The body completed. Stop.
			return;
		} catch (AsyncActionSubmittedInterrupt a) {
			// OK. Suspend the logic until the async's callback wakes us back up.
		}
	}

	public <A,R> R execute(final Task<A, R> task) {
		return execute(task, null);
	}
	
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
			throw new AsyncActionSubmittedInterrupt();
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