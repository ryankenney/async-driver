package info.ryankenney.jasync_driver.example;

import info.ryankenney.jasync_driver.JasyncDriver;
import info.ryankenney.jasync_driver.AsyncTask;
import info.ryankenney.jasync_driver.DriverBody;
import info.ryankenney.jasync_driver.ResultHandler;
import info.ryankenney.jasync_driver.SyncTask;
import info.ryankenney.jasync_driver.example.supporting.Permissions;
import info.ryankenney.jasync_driver.example.supporting.ReturnCallback;
import info.ryankenney.jasync_driver.example.supporting.Status;
import info.ryankenney.jasync_driver.example.supporting.User;
import info.ryankenney.jasync_driver.example.supporting.UserInterface;
import info.ryankenney.jasync_driver.example.supporting.UserInterface;
import info.ryankenney.jasync_driver.example.supporting.WebServer;
import info.ryankenney.jasync_driver.example.supporting.WebServer;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExampleAppWithLogging {

	Executor browserThread;
	WebServer webServer;
	UserInterface userInterface;
	User user = new User("brad");

	public ExampleAppWithLogging(Executor browserThread)  {
		this.browserThread = browserThread;
		this.webServer = new WebServer(browserThread);
		this.userInterface = new UserInterface(browserThread);
	}

	public void onUserClick(final Runnable onComplete) {
		
		final AsyncTask<User,Permissions> readUserPermissions = new AsyncTask<User,Permissions>() {
			public void run(final User user, final ResultHandler<Permissions> resultHandler) {
				webServer.readUserPermissions(user, new ReturnCallback<Permissions> () {
					public void handleResult(Permissions result) {
						System.out.println("== Executing [readUserPermissions] ==");
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
				userInterface.promptForNewValue(new ReturnCallback<String> () {
					public void handleResult(String result) {
						System.out.println("== Executing [promptUserForNewValue] ==");
						resultHandler.reportComplete(result);
					}
				});
			}
		};

		final AsyncTask<String,Status> updateStoredValue = new AsyncTask<String,Status>() {
			public void run(final String value, final ResultHandler<Status> resultHandler) {
				webServer.storeValue(value, new ReturnCallback<Status> () {
					public void handleResult(Status result) {
						System.out.println("== Executing [updateStoredValue] ==");
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

		final JasyncDriver driver = new JasyncDriver();
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
		}, onComplete);
	}
	
	public static void main(String[] ags) {
		final ExecutorService browserThread = Executors.newFixedThreadPool(1);
		new ExampleAppWithLogging(browserThread).onUserClick(new Runnable() {
			public void run() {
				browserThread.shutdown();
			}
		});
	}
}
