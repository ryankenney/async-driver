package info.ryankenney.jasync_driver.example.supporting;

import java.util.concurrent.Executor;


public class UserInterface {
	
	private Executor browserThread;
	
	public UserInterface(Executor browserThread) {
		this.browserThread = browserThread;
	}
	
	public void showError(String message) {
		System.out.println("[UI] Notify User: "+message);
	}

	public void promptForNewValue(final ReturnCallback<String> returnCallback)  {
		browserThread.execute(new Runnable() {
			public void run() {
				System.out.println("[UI] Ask User for New Value");
				browserThread.execute(new Runnable() {
					public void run() {
						String result = "foobar";
						System.out.println("[UI] User Provided New Value: "+result);
						returnCallback.handleResult(result);
					}
				});
			}
		});
	}
}
