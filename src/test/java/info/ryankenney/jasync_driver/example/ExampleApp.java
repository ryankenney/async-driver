package info.ryankenney.jasync_driver.example;

import info.ryankenney.jasync_driver.AsyncTask;
import info.ryankenney.jasync_driver.DriverBody;
import info.ryankenney.jasync_driver.JasyncDriver;
import info.ryankenney.jasync_driver.ResultHandler;
import info.ryankenney.jasync_driver.SyncTask;
import info.ryankenney.jasync_driver.example.supporting.Permissions;
import info.ryankenney.jasync_driver.example.supporting.ReturnCallback;
import info.ryankenney.jasync_driver.example.supporting.Status;
import info.ryankenney.jasync_driver.example.supporting.User;
import info.ryankenney.jasync_driver.example.supporting.UserInterface;
import info.ryankenney.jasync_driver.example.supporting.WebServer;

/**
 * An example application that uses jasync-driver.
 * 
 * @author rkenney
 */
public class ExampleApp {

	WebServer webServer;
	UserInterface userInterface;
	User user = new User("brad");

	public ExampleApp(WebServer webServer, UserInterface userInterface) {
		this.webServer = webServer;
		this.userInterface = userInterface;
	}

	public void onUserClick(Runnable onComplete) {
		
		/* ===== All actions wrapped in AsyncTask/SyncTask ===== */ 
		
		final AsyncTask<User,Permissions> readUserPermissions = new AsyncTask<User,Permissions>() {
			public void run(final User user, final ResultHandler<Permissions> resultHandler) {
				webServer.readUserPermissions(user, new ReturnCallback<Permissions> () {
					public void handleResult(Permissions result) {
						resultHandler.reportComplete(result);
					}
				});
			}
		};

		final SyncTask<Permissions,Boolean> hasEditPermission = new SyncTask<Permissions,Boolean>() {
			public Boolean run(Permissions permissions) {
				return permissions.toString().contains("edit");
			}
		};

		final SyncTask<Void,Void> notifyPermissionsError = new SyncTask<Void,Void>() {
			public Void run(Void arg) {
				userInterface.showError("User does not have edit permission");
				return null;
			}
		};

		final AsyncTask<Void,String> promptUserForNewValue = new AsyncTask<Void,String>() {
			public void run(final Void  arg, final ResultHandler<String> resultHandler) {
				userInterface.promptForNewValue(new ReturnCallback<String> () {
					public void handleResult(String result) {
						resultHandler.reportComplete(result);
					}
				});
			}
		};

		final AsyncTask<String,Status> updateStoredValue = new AsyncTask<String,Status>() {
			public void run(final String value, final ResultHandler<Status> resultHandler) {
				webServer.storeValue(value, new ReturnCallback<Status> () {
					public void handleResult(Status result) {
						resultHandler.reportComplete(result);
					}
				});
			}
		};

		final SyncTask<Void,Void> notifyStoreError = new SyncTask<Void,Void>() {
			public Void run(Void arg) {
				userInterface.showError("Store action failed");
				return null;
			}
		};

		/* ===== The main driver logic ===== */ 
		
		// ATTENTION: Familiarize yourself with the rules of DriverBody before
		// editing this block. DriverBody.run() is executed repeatedly,
		// with the result of Task executions read from cache.
		final JasyncDriver driver = new JasyncDriver(onComplete);
		driver.execute(new DriverBody() {
			public void run() {
				Permissions permissions = driver.execute(readUserPermissions, user);
				if (!driver.execute(hasEditPermission, permissions)) {
					driver.execute(notifyPermissionsError);
				} else {
					String userInput = driver.execute(promptUserForNewValue);
					Status storeStatus = driver.execute(updateStoredValue, userInput);
					if (storeStatus != Status.OK) {
						driver.execute(notifyStoreError);
					}
				}
			}
		});
	}
}
