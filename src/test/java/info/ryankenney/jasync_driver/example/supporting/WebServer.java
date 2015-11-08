package info.ryankenney.jasync_driver.example.supporting;

import java.util.concurrent.Executor;

public class WebServer {
	
	private Executor browserThread;

	public WebServer(Executor browserThread) {
		this.browserThread = browserThread;
	}
	
	public void readUserPermissions(User  user, final ReturnCallback<Permissions> returnCallback) {
		System.out.println("[WEB SERVER] Requesting User Permissions for: "+user);
		browserThread.execute(new Runnable() {
			public void run() {
				Permissions result = new Permissions("view,edit");
				System.out.println("[WEB SERVER] Recieved User Permissions: "+result);
				returnCallback.handleResult(result);
			}
		});
	}
	
	public void storeValue(String value,  final ReturnCallback<Status> returnCallback) {
		System.out.println("[WEB SERVER] Requesting Store Value: "+value);
		browserThread.execute(new Runnable() {
			public void run() {
				Status result = Status.FAILURE;
				System.out.println("[WEB SERVER] Received Store Value Result: "+result);
				returnCallback.handleResult(result);
			}
		});
	}
}
