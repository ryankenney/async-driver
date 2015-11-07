package info.ryankenney.async_driver.example.supporting;

import java.util.concurrent.Executor;

public class WebServerImpl implements WebServer {
	
	private Executor browserThread;

	public WebServerImpl(Executor browserThread) {
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
