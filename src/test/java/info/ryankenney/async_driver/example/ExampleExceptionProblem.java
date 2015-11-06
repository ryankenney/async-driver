package info.ryankenney.async_driver.example;

import info.ryankenney.async_driver.AsyncDriver;
import info.ryankenney.async_driver.AsyncTask;
import info.ryankenney.async_driver.DriverBody;
import info.ryankenney.async_driver.ResultHandler;
import info.ryankenney.async_driver.SyncTask;

import java.util.ArrayList;
import java.util.List;

public class ExampleExceptionProblem {

	static interface User {
		public String getName();
	}
	
	static class Permissions {
		public List<String> permissions = new ArrayList<>();
	}

	static interface WebServer {
		public Permissions readUserPermissions(User  user, ReturnCallback<Permissions> permission);

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
		
		final AsyncTask<User,Permissions> readUserPermissions = new AsyncTask<User,Permissions>() {
			public void run(final User user, final ResultHandler<Permissions> resultHandler) {
				webServer.readUserPermissions(user, new ReturnCallback<Permissions> () {
					public void handleResult(Permissions result) {
						resultHandler.reportComplete(result);
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

		final AsyncDriver driver = new AsyncDriver();
		driver.execute(new DriverBody() {
			public void run() {
				// ERROR: Do not use try/catch blocks within the DriveBody.
				// Instead, incorporate try/catch logic into the body of one or
				// more AsyncTask/SyncTax.
				try {
					driver.execute(readUserPermissions, user);
				} catch (Exception e) {
					driver.execute(notifyUserOfReadError);
				}
			}
		});
	}

}
