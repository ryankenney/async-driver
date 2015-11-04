package info.ryankenney.async_driver;

import java.util.ArrayList;
import java.util.List;

public class AsyncDriver {

	public AsyncDriver() {
		// TODO Auto-generated constructor stub
	}
	
	private DriverBody body;
	private List<HistoryEntry> historyOfExecutedTasks = new ArrayList<>();
	int stepInLogicGraph;

	public AsyncDriver(DriverBody body) {
		this.body = body;
	}
	
	public void execute(DriverBody driverBody) {
		this.body = driverBody;
		stepInLogicGraph = 0;
		try {
			driverBody.run();
			return;
		} catch (AsyncActionSubmittedInterrupt a) {
			// Ok. We're just suspending the logic until the async's callback wakes us back up.
		}
	}
	
	public <A,R> R execute(final AsyncTask<A,R> task, final A arg)  {
		if (stepInLogicGraph < historyOfExecutedTasks.size()) {
			HistoryEntry previousTaskExecution = historyOfExecutedTasks.get(stepInLogicGraph);
			if (task != previousTaskExecution.task) {
				// TODO [rkenney]: Better exception
				throw new RuntimeException("Something went wrong...");
			}
			stepInLogicGraph++;
			@SuppressWarnings("unchecked")
			R result = (R) previousTaskExecution.result;
			return result;
		}
		task.run(arg, new ResultHandler<R>() {
			@Override
			public void reportComplete(R result) {
				historyOfExecutedTasks.add(new HistoryEntry(task, result));
				execute(body);
			}
		});
		throw new AsyncActionSubmittedInterrupt();
	}
	
	private static class HistoryEntry {
		private Task task;
		private Object result;

		HistoryEntry(Task task, Object result) {
			this.task = task;
			this.result = result;
		}
	}
}