package info.ryankenney.jasync_driver.example.supporting;

import java.util.concurrent.Executor;

public class WebServer {
	
	private Executor browserThread;

	public WebServer(Executor browserThread) {
		this.browserThread = browserThread;
	}
	
	public void readUserPermissions(User  user, final ReturnCallback<Permissions> returnCallback) {
		browserThread.execute(new Runnable() {
			public void run() {
				returnCallback.handleResult(new Permissions("view,edit"));
			}
		});
	}
	
	public void storeValue(String value,  final ReturnCallback<Status> returnCallback) {
		browserThread.execute(new Runnable() {
			public void run() {
				returnCallback.handleResult(Status.FAILURE);
			}
		});
	}
}
