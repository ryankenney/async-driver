package info.ryankenney.jasync_driver.example.supporting;

import java.util.concurrent.Executor;


public class UserInterface {
	
	private Executor browserThread;
	
	public UserInterface(Executor browserThread) {
		this.browserThread = browserThread;
	}
	
	public void showError(String message) {
	}

	public void promptForNewValue(final ReturnCallback<String> returnCallback)  {
		browserThread.execute(new Runnable() {
			public void run() {
				returnCallback.handleResult("foobar");
			}
		});
	}
}
