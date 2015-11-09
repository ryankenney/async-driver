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
 * An example application that uses jasync-driver with detailed logging to
 * demonstrate the internal program flow.
 * 
 * @author rkenney
 */
public class ExampleAppWithLogging {

	WebServer webServer;
	UserInterface userInterface;
	User user = new User("brad");

	public ExampleAppWithLogging(WebServer webServer, UserInterface userInterface) {
		this.webServer = webServer;
		this.userInterface = userInterface;
	}

	/* ===== All actions wrapped in AsyncTask/SyncTask ===== */ 
	
	public void onUserClick(final Runnable onComplete) {
		
		final AsyncTask<User,Permissions> readUserPermissions = new AsyncTask<User,Permissions>() {
			public void run(final User user, final ResultHandler<Permissions> resultHandler) {
				System.out.println("== Executing [readUserPermissions] ==");
				webServer.readUserPermissions(user, new ReturnCallback<Permissions> () {
					public void handleResult(Permissions result) {
						resultHandler.reportComplete(result);
					}
				});
			}
		};

		final SyncTask<Permissions,Boolean> hasEditPermission = new SyncTask<Permissions,Boolean>() {
			public Boolean run(Permissions permissions) {
				System.out.println("== Executing [hasEditPermission] ==");
				return permissions.toString().contains("edit");
			}
		};

		final SyncTask<Void,Void> notifyPermissionsError = new SyncTask<Void,Void>() {
			public Void run(Void arg) {
				System.out.println("== Executing [notifyPermissionsError] ==");
				userInterface.showError("User does not have edit permission");
				return null;
			}
		};

		final AsyncTask<Void,String> promptUserForNewValue = new AsyncTask<Void,String>() {
			public void run(final Void  arg, final ResultHandler<String> resultHandler) {
				System.out.println("== Executing [promptUserForNewValue] ==");
				userInterface.promptForNewValue(new ReturnCallback<String> () {
					public void handleResult(String result) {
						resultHandler.reportComplete(result);
					}
				});
			}
		};

		final AsyncTask<String,Status> updateStoredValue = new AsyncTask<String,Status>() {
			public void run(final String value, final ResultHandler<Status> resultHandler) {
				System.out.println("== Executing [updateStoredValue] ==");
				webServer.storeValue(value, new ReturnCallback<Status> () {
					public void handleResult(Status result) {
						resultHandler.reportComplete(result);
					}
				});
			}
		};

		final SyncTask<Void,Void> notifyStoreError = new SyncTask<Void,Void>() {
			public Void run(Void arg) {
				System.out.println("== Executing [notifyStoreError]  ==");
				userInterface.showError("Store action failed!");
				return null;
			}
		};

		/* ===== The main driver logic ===== */ 
		
		// ATTENTION: Familiarize yourself with the rules of DriverBody before
		// editing this block. The DriverBody.body() is recursively executed repeatedly,
		// with the result of Task executions read from cache.
		final JasyncDriver driver = new JasyncDriver(onComplete);
		driver.execute(new DriverBody() {
			public void run() {
				System.out.printf("Launching DriverBody%n");
				Permissions permissions = driver.execute(readUserPermissions, user);
				System.out.printf("Value of permissions: %s%n", permissions);
				Boolean hasPermission = driver.execute(hasEditPermission, permissions);
				System.out.printf("Value of hasPermission: %s%n", hasPermission);
				if (!hasPermission) {
					driver.execute(notifyPermissionsError);
				} else {
					String userInput = driver.execute(promptUserForNewValue);
					System.out.printf("Value of userInput: %s%n", userInput);
					Status storeStatus = driver.execute(updateStoredValue, userInput);
					System.out.printf("Value of storeStatus: %s%n", storeStatus);
					if (storeStatus != Status.OK) {
						driver.execute(notifyStoreError);
					}
				}
			}
		});
	}
}
