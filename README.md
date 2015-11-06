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

Critical Rules of the Road
--------------------

* **Wrap all reads/writes of non-local data in AsyncTasks/SyncTasks**
	* ...
* **Don't use try/catch in the AsyncDriver body**
	* ...

### Wrap all reads/writes of non-local data in AsyncTasks/SyncTasks

Here is an example of what not to do (see comments):

```java
		final AsyncDriver driver = new AsyncDriver();
		driver.execute(new DriverBody() {
			public void run() {
				Permissions permissions = driver.execute(readUserPermissions, user);
				// ERROR: Accessing a class variable! (defined outside of the DriverBody method)
				// This needs to be wrapped in a SyncTask
				if (updateServerValueCheckbox.isSet()) {
					// WARNING: This method may or may access external variables
					// within. We can't tell from here, so it's safest to wrap
					// it in a SyncTask to ensure that edits to the method body do
					// not break the DriverBody.
					if (hasEditPermission(permissions)) {
						driver.execute(notifyPermissionsError);
					} else {
						String userInput = driver.execute(promptUserForNewValue);
						Status storeStatus = driver.execute(updateStoredValue, userInput);
						if (storeStatus != Status.OK) {
							driver.execute(notifyStoreError);
						}
					}
				}
			}
		});
```

async-driver can only function properly if all access to variables outside of the **method scope** is wrapped by
AsyncTask/SyncTask and executed by driver.execute(Task).


### Don't use try/catch in the AsyncDriver body

Never use try/catch blocks to cature exceptions escaping from AsyncTask/SyncTask. async-driver uses exceptions (Error)
to interrupt execution when waiting for a response from an asynchronus action, so they need to be able to leak
out of the DriverBody.

Here is an example of what not to do:

```java
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
```

Here's a fixed version of that code:

```java
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
```

And here's a more complete version of the fix, which shows the exception handling inside of the AsyncTask
(see comments):

```java
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
```

How it Works
--------------------

The general rule: **Any read/write actions that interact with data outside of method scoped variables of the DriverBody
need to be wrapped in Tasks.**

This is because the result of each Task is cached and replayed as the DriverBody is re-executed on the return of
each asynchronous action. For example, looking at the first code sample above, you can see that the DriverBody
leverages three asynchronous Tasks:

* readUserPermissions
* promptUserForNewValue
* updateStoredValue

This means that the async-driver will cause the following order of execution:

* DriverBody executes logic until "readUserPermissions".
* **DriverBody executes "readUserPermissions"**, and then returns, awaiting an asynchrnous callback to wake it up.
* The webServer triggers the callback within "readUserPermissions", waking up the async-driver.
* DriverBody executes logic until "readUserPermissions", loading the cached result for this Task.
* DriverBody executes logic until "promptUserForNewValue".
* **DriverBody executes "promptUserForNewValue"**, and then returns, awaiting an asynchrnous callback to wake it up.
* The userInterface triggers the callback within "readUserPermissions", waking up the async-driver.
* DriverBody executes logic until "readUserPermissions", loading the cached result for this Task.
* DriverBody executes logic until "promptUserForNewValue", loading the cached result for this Task.
* DriverBody executes logic until "updateStoredValue".
* **DriverBody executes "updateStoredValue"**, and then returns, awaiting an asynchrnous callback to wake it up.
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

