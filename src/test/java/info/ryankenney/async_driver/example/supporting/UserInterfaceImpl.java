package info.ryankenney.async_driver.example.supporting;

import java.util.concurrent.Executor;


public class UserInterfaceImpl implements UserInterface {
	
	private Executor browserThread;
	
	public UserInterfaceImpl(Executor browserThread) {
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
