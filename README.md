async-driver
====================

The Problem
--------------------

Imagine that you want to model this logic in a java gui:

* On User Click
	* Read User Permissions
	* If User Has "edit" Permissions
		* Prompt User for New Value
		* Store New Value
		* If Store Fails
			* Notify User of Failure
	* If User Does Not Have "edit" Permissions
		* Notify User of Permissions Issue

Because many of these actions are asynchronous (they require a server or user response before resuming), you can end up with code that looks like this:

```java
	public void onUserClick() {
		readUserPermissions();
	}

	void readUserPermissions() {
		webServer.readUserPermissions(user, new ReturnCallback<Permissions> () {
			public void handleResult(Permissions permissions) {
				if (permissions.permissions.contains("edit")) {
					promptUserForNewValue();
				} else {
					notifyPermissionsError();
				}
			}
		});
	}

	void notifyPermissionsError() {
		userInterface.showError("User does not have edit permission");
	}

	void promptUserForNewValue() {
		userInterface.promptForNewValue(new ReturnCallback<String> () {
			@Override
			public void handleResult(String result) {
				updateStoredValue(result);
			}
		});
	}

	void updateStoredValue(String value) {
		webServer.storeValue(value, new ReturnCallback<Status> () {
			@Override
			public void handleResult(Status result) {
				if (result != Status.OK) {
					notifyStoreError();
				}
			}
		});
	}

	void notifyStoreError() {
		userInterface.showError("Store action failed");
	}
```

Where did that nice little block of conditional logic go? It got smeared across all of the callback methods necesssary to string the asynchronous actions together.

A Solution (async-driver)
--------------------

With async-driver, we can define the logic block as if everything is synchronous.
For example, this models the logic above:

```java
	final AsyncDriver driver = new AsyncDriver();
	driver.execute(new DriverBody() {
		public void run() {
			
			Permissions permissions = driver.execute(readUserPermissions, user);
			if (driver.execute(hasEditPermission, permissions)) {
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
```

From here, you have to wrap each of the asynchronous actions in a Task object,
but the resulting code is arguably much more legible. Here is a more complete example:

```java
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

	final SyncTask<Permissions,Boolean> hasEditPermission = new SyncTask<Permissions,Boolean>() {
		public Boolean run(Permissions permissions) {
			return permissions.permissions.contains("edit");
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

	final AsyncDriver driver = new AsyncDriver();
	driver.execute(new DriverBody() {
		public void run() {
			
			Permissions permissions = driver.execute(readUserPermissions, user);
			if (driver.execute(hasEditPermission, permissions)) {
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
```

Considerations
--------------------

The general rule: *Any read/write actions that interact with data outside of method-scoped variables of the DriverBody
need to be wrapped in Tasks.*


This is because the result of each Task is cached and replayed as the DriverBody is re-executed on the return of
each asynchronous action. For example, looking at the previous code sample, you can see that the DriverBody
leverages three asynchronous Tasks:

* readUserPermissions
* promptUserForNewValue
* updateStoredValue

This means that the async-driver will cause the following order of execution:

* DriverBody executes logic until "readUserPermissions".
* DriverBody executes "readUserPermissions", and then returns, awaiting an asynchrnous callback to wake it up.
* The webServer triggers the callback within "readUserPermissions", waking up the async-driver.
* DriverBody executes logic until "readUserPermissions", loading the cached result for this Task.
* DriverBody executes logic until "promptUserForNewValue".
* DriverBody executes "promptUserForNewValue", and then returns, awaiting an asynchrnous callback to wake it up.
* The userInterface triggers the callback within "readUserPermissions", waking up the async-driver.
* DriverBody executes logic until "readUserPermissions", loading the cached result for this Task.
* DriverBody executes logic until "promptUserForNewValue", loading the cached result for this Task.
* DriverBody executes logic until "updateStoredValue".
* DriverBody executes "updateStoredValue", and then returns, awaiting an asynchrnous callback to wake it up.
* The webServer triggers the callback within "updateStoredValue", waking up the async-driver.
* DriverBody executes logic until "readUserPermissions", loading the cached result for this Task.
* DriverBody executes logic until "promptUserForNewValue", loading the cached result for this Task.
* DriverBody executes logic until "updateStoredValue", loading the cached result for this Task.
* DriverBody executes the remaining logic, hitting no aditional async tasks, and returns.

If an external resource was read without being wrapped by a Task, it would be execute many more times than expected by the developer,
and the result could change from run to run, causing non-obvious behavior.

Similarly, if an external resource was written without being wrapped by a Task, it would be execute many more times than expected by the developer,
and the result could change from run to run, causing non-obvious behavior.

async-executor includes some runtime consistency checks to avoid these situations,
but it is important that users of this library be made aware of its limitations.



