package info.ryankenney.async_driver.example;

import info.ryankenney.async_driver.AsyncDriver;
import info.ryankenney.async_driver.AsyncTask;
import info.ryankenney.async_driver.DriverBody;
import info.ryankenney.async_driver.ResultHandler;
import info.ryankenney.async_driver.SyncTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ExampleExceptionProblemWorkaround {

	static interface User {
		public String getName();
	}
	
	static class Permissions {
		public List<String> permissions = new ArrayList<>();
	}

	static interface WebServer {
		public void readUserPermissions(User  user, ReturnCallback<Permissions> permission);

		public void storeValue(String value,  ReturnCallback<Status> returnCallback);
	}

	static interface CheckboxWidget {
		public boolean isSet();
	}

	static interface UserInterface {
		public void showError(String message);
		public void promptForNewValue(ReturnCallback<String> returnCallback);
	}

	static interface ReturnCallback<R> {
		public void handleResult(R result);
	}
	
	static enum Status {
		OK,
		FAILURE
	}
	
	WebServer webServer;
	UserInterface userInterface;
	User user;
	
	public void onUserClick() {

		final AtomicBoolean lastRequestFailed = new AtomicBoolean(false);
		
		final AsyncTask<User,Permissions> readUserPermissions = new AsyncTask<User,Permissions>() {
			public void run(final User user, final ResultHandler<Permissions> resultHandler) {
				webServer.readUserPermissions(user, new ReturnCallback<Permissions> () {
					public void handleResult(Permissions result) {
						// Exception handling has been moved inside of this AsyncTask
						try {
							resultHandler.reportComplete(result);
						} catch (Exception e) {
							// The exceptional result is stored in an external variable.
							// That is fine because we're using AsyncTask/SyntTasks
							// to access the variable.
							lastRequestFailed.set(true);							
						}
					}
				});
			}
		};

		final SyncTask<Void,Void> notifyUserOfReadError = new SyncTask<Void,Void>() {
			public Void run(Void arg) {
				userInterface.showError("Failed to read from server");
				return null;
			}
		};
		
		final SyncTask<Void,Boolean> getLastRequestFailed = new SyncTask<Void,Boolean>() {
			public Boolean run(Void arg) {
				return lastRequestFailed.get();
			}
		};
		
		final AsyncDriver driver = new AsyncDriver();
		driver.execute(new DriverBody() {
			public void run() {
				// NOTE: Now now exception handling here,
				// but instead it is inside of readUserPermissions
				driver.execute(readUserPermissions, user);
				if (driver.execute(getLastRequestFailed)) {
					driver.execute(notifyUserOfReadError);
				}
			}
		});
	}
}
